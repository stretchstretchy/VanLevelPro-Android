package com.vanlevelpro.app.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import com.vanlevelpro.app.model.ConnectionState
import com.vanlevelpro.app.model.Telemetry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

class BluetoothManager(private val context: Context) {

    companion object {
        const val DEVICE_NAME = "VanLevel Pro"
    }

    enum class OtaState {
        IDLE, STARTING, TRANSFERRING, FINISHING, SUCCESS, FAILED
    }

    //--------------------------------------------------
    // Android Bluetooth
    //--------------------------------------------------

    private val systemBluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager

    private val adapter: BluetoothAdapter?
        get() = systemBluetoothManager.adapter

    //--------------------------------------------------
    // Scanner / Connection
    //--------------------------------------------------

    private var scanner: BleScanner? = null
    private var connection: BleConnection? = null

    // Heartbeat: the firmware can't always rely on the phone's BLE stack
    // telling it about a disconnect (seen ghosting the link on some
    // Samsung devices), so it watches for a periodic write here and
    // force-drops the link itself if this goes quiet. Keep this well
    // under the firmware's HEARTBEAT_TIMEOUT_MS (15s).
    private val heartbeatHandler =
        android.os.Handler(android.os.Looper.getMainLooper())

    private val heartbeatIntervalMs = 4000L

    private val heartbeatRunnable = object : Runnable {
        override fun run() {

            val sent = connection?.writeCommand("{\"cmd\":\"ping\"}")

            Log.e("TEST", "Heartbeat ping sent=$sent")

            heartbeatHandler.postDelayed(this, heartbeatIntervalMs)
        }
    }

    private fun startHeartbeat() {
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
        heartbeatHandler.postDelayed(heartbeatRunnable, heartbeatIntervalMs)
    }

    private fun stopHeartbeat() {
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
    }

    //--------------------------------------------------
    // RX watchdog
    //--------------------------------------------------
    // Android's own onConnectionStateChange can fail to fire promptly
    // (or at all, in practice) when the ESP32 loses power abruptly
    // rather than disconnecting gracefully - there's no link-layer
    // event for the OS to react to. Telemetry normally streams every
    // ~100ms, so if nothing at all has arrived in RX_TIMEOUT_MS while
    // we still think we're CONNECTED, the link is actually dead and we
    // need to force our own reconnect rather than trust the OS's state.

    private val rxWatchdogHandler =
        android.os.Handler(android.os.Looper.getMainLooper())

    private val rxWatchdogIntervalMs = 5000L
    private val rxTimeoutMs = 15000L

    @Volatile
    private var lastRxAt: Long = 0

    private val rxWatchdogRunnable = object : Runnable {
        override fun run() {

            if (_connectionState.value == ConnectionState.CONNECTED &&
                System.currentTimeMillis() - lastRxAt > rxTimeoutMs
            ) {
                Log.e("TEST", "RX watchdog - no data from ESP32 in ${rxTimeoutMs}ms, forcing reconnect")

                disconnect()
                scan()

            } else {
                rxWatchdogHandler.postDelayed(this, rxWatchdogIntervalMs)
            }
        }
    }

    private fun startRxWatchdog() {
        lastRxAt = System.currentTimeMillis()
        rxWatchdogHandler.removeCallbacks(rxWatchdogRunnable)
        rxWatchdogHandler.postDelayed(rxWatchdogRunnable, rxWatchdogIntervalMs)
    }

    private fun stopRxWatchdog() {
        rxWatchdogHandler.removeCallbacks(rxWatchdogRunnable)
    }

    //--------------------------------------------------
    // Remembered device (for direct reconnect, bypassing scan)
    //--------------------------------------------------

    // Android's BLE scan cache/name resolution has proven unreliable in
    // the field (stale results after OS updates, missing device names).
    // Once we've successfully connected to a device once, we remember
    // its MAC address and attempt a direct connectGatt() to it first on
    // future launches - this bypasses scanning (and its caching quirks)
    // entirely. Scanning remains as the fallback for first-time pairing
    // or if the remembered device can't be reached.
    private val connectionPrefs =
        context.getSharedPreferences("VanLevelConnection", Context.MODE_PRIVATE)

    private var lastKnownAddress: String?
        get() = connectionPrefs.getString("last_device_address", null)
        set(value) {
            connectionPrefs.edit().putString("last_device_address", value).apply()
        }

    private val directConnectHandler =
        android.os.Handler(android.os.Looper.getMainLooper())

    private var directConnectTimeoutRunnable: Runnable? = null

    private val directConnectTimeoutMs = 6000L

    fun forgetRememberedDevice() {
        lastKnownAddress = null
    }

    private val _connectionState =
        MutableStateFlow(ConnectionState.DISCONNECTED)

    val connectionState: StateFlow<ConnectionState> =
        _connectionState.asStateFlow()

    private val _status =
        MutableStateFlow("Disconnected")

    val status: StateFlow<String> =
        _status.asStateFlow()

    private val _telemetry =
        MutableStateFlow(Telemetry())

    val telemetry: StateFlow<Telemetry> =
        _telemetry.asStateFlow()

    //--------------------------------------------------
    // OTA firmware update
    //--------------------------------------------------

    private val _otaState =
        MutableStateFlow(OtaState.IDLE)

    val otaState: StateFlow<OtaState> =
        _otaState.asStateFlow()

    // (bytesSent, totalSize) - drives a progress bar in the UI.
    private val _otaProgress =
        MutableStateFlow(0 to 0)

    val otaProgress: StateFlow<Pair<Int, Int>> =
        _otaProgress.asStateFlow()

    private var otaStartAck: CompletableDeferred<Boolean>? = null
    private var otaEndAck: CompletableDeferred<Boolean>? = null

    //--------------------------------------------------
    // Scan
    //--------------------------------------------------

    fun scan() {

        Log.e("TEST", "BluetoothManager.scan() CALLED")

        val bluetoothAdapter = adapter

        if (bluetoothAdapter == null) {
            _status.value = "Bluetooth not supported"
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            _status.value = "Bluetooth is OFF"
            return
        }

        val remembered = lastKnownAddress

        if (remembered != null) {

            Log.e("TEST", "Attempting direct reconnect to remembered device $remembered")

            _status.value = "Reconnecting..."

            val device =
                try {
                    bluetoothAdapter.getRemoteDevice(remembered)
                } catch (e: IllegalArgumentException) {
                    null
                }

            if (device != null) {

                connect(device)

                // If the remembered device isn't actually reachable
                // (sold/replaced board, out of range, etc.) fall back
                // to a full scan rather than hanging forever.
                directConnectTimeoutRunnable?.let {
                    directConnectHandler.removeCallbacks(it)
                }

                directConnectTimeoutRunnable = Runnable {

                    if (_connectionState.value != ConnectionState.CONNECTED) {

                        Log.e("TEST", "Direct reconnect timed out - falling back to scan")

                        connection?.disconnect()
                        connection = null

                        startScan(bluetoothAdapter)
                    }
                }

                directConnectHandler.postDelayed(
                    directConnectTimeoutRunnable!!,
                    directConnectTimeoutMs
                )

                return
            }
        }

        startScan(bluetoothAdapter)
    }

    private fun startScan(bluetoothAdapter: BluetoothAdapter) {

        scanner?.stop()

        _connectionState.value = ConnectionState.SCANNING

        scanner = BleScanner(

            adapter = bluetoothAdapter,

            onDeviceFound = { device: BluetoothDevice ->

                scanner?.stop()

                connect(device)
            },

            onStatus = { message ->

                Log.e("TEST", "Scanner: $message")
                _status.value = message
            }
        )

        scanner?.start()
    }

    //--------------------------------------------------
    // Connect
    //--------------------------------------------------

    private fun connect(device: BluetoothDevice) {

        Log.e("TEST", "Connecting to ${device.address}")

        _connectionState.value = ConnectionState.CONNECTING

        stopHeartbeat()

        connection?.disconnect()

        connection = BleConnection(

            context = context,

            device = device,

            onStatus = { message ->

                Log.e("TEST", "Connection: $message")

                _status.value = message

                if (message == "Connected") {
                    _connectionState.value = ConnectionState.CONNECTED
                    startHeartbeat()
                    startRxWatchdog()

                    directConnectTimeoutRunnable?.let {
                        directConnectHandler.removeCallbacks(it)
                    }
                    directConnectTimeoutRunnable = null

                    lastKnownAddress = device.address
                }
            },

            onTelemetry = { json ->

                Log.e("TEST", "RX: $json")

                lastRxAt = System.currentTimeMillis()

                _status.value = json
                handleIncomingMessage(json)
            }
        )

        connection?.connect()
    }

    //--------------------------------------------------
    // Parse JSON
    //--------------------------------------------------

    private fun handleIncomingMessage(json: String) {

        try {

            val obj = JSONObject(json)

            when (obj.optString("type")) {
                "telemetry" -> parseTelemetry(obj)
                "ota_status" -> parseOtaStatus(obj)
                // "hello"/"version"/"status"/"error" etc. are only ever
                // sent in direct response to a command the app itself
                // issued (e.g. Diagnostics requesting a status refresh)
                // and aren't tracked as ongoing state here - callers
                // read them directly off the raw `status` string.
            }

        } catch (e: Exception) {

            Log.e("TEST", "JSON ERROR", e)

            _status.value = json
        }
    }

    private fun parseTelemetry(obj: JSONObject) {

        _telemetry.value = Telemetry(

            pitch = obj.optDouble("pitch", 0.0).toFloat(),

            // Roll comes back inverted relative to the physical
            // direction of lean on this sensor mounting - negate it
            // here rather than touching the firmware.
            roll = -obj.optDouble("roll", 0.0).toFloat(),

            connected = true,

            calibrated = obj.optBoolean("calibrated", false)
        )

        Log.e(
            "TEST",
            "Telemetry Updated Pitch=${_telemetry.value.pitch} Roll=${_telemetry.value.roll}"
        )
    }

    private fun parseOtaStatus(obj: JSONObject) {

        val state = obj.optString("state")
        val written = obj.optInt("written", 0)
        val total = obj.optInt("total", 0)

        Log.e("TEST", "OTA status: state=$state written=$written total=$total")

        when (state) {

            "started" -> {
                otaStartAck?.complete(true)
                otaStartAck = null
            }

            "progress" -> {
                // Firmware's own confirmation of what it's actually
                // written - authoritative over the app's own
                // bytes-sent tracking, so let it correct the progress
                // bar if the two ever drift.
                _otaProgress.value = written to total
            }

            "complete" -> {
                otaEndAck?.complete(true)
                otaEndAck = null
            }

            "failed" -> {
                // Could be a failed ota_start OR a failed ota_end -
                // complete whichever ack is actually outstanding.
                otaStartAck?.complete(false)
                otaStartAck = null
                otaEndAck?.complete(false)
                otaEndAck = null
            }
        }
    }

    //--------------------------------------------------
    // Disconnect
    //--------------------------------------------------

    fun disconnect() {

        Log.e("TEST", "Disconnect()")

        stopHeartbeat()
        stopRxWatchdog()

        directConnectTimeoutRunnable?.let {
            directConnectHandler.removeCallbacks(it)
        }
        directConnectTimeoutRunnable = null

        scanner?.stop()

        connection?.disconnect()

        connection = null

        _telemetry.value = Telemetry()

        _status.value = "Disconnected"

        _connectionState.value = ConnectionState.DISCONNECTED
    }

    //--------------------------------------------------
    // Send Command
    //--------------------------------------------------

    fun send(json: String) {

        val sent = connection?.writeCommand(json)

        Log.e("TEST", "send($json) -> sent=$sent")
    }

    //--------------------------------------------------
    // Calibrate
    //--------------------------------------------------

    fun calibrate() {

        Log.e("TEST", "Calibrate() requested")

        send("{\"cmd\":\"calibrate\"}")
    }

    //--------------------------------------------------
    // OTA Firmware Update
    //--------------------------------------------------

    /**
     * Drives a full OTA transfer: sends ota_start, streams the firmware
     * in chunks (each confirmed before the next is sent), then sends
     * ota_end. Returns true only if the ESP32 confirms the completed
     * image verified successfully. Safe to call from any coroutine -
     * suspends for the whole transfer, so callers should launch this
     * from their own scope rather than blocking a UI thread directly.
     */
    suspend fun performOtaUpdate(firmware: ByteArray): Boolean {

        val conn = connection

        if (conn == null || _connectionState.value != ConnectionState.CONNECTED) {
            Log.e("TEST", "performOtaUpdate() aborted - not connected")
            _otaState.value = OtaState.FAILED
            return false
        }

        val totalSize = firmware.size

        _otaState.value = OtaState.STARTING
        _otaProgress.value = 0 to totalSize

        //--------------------------------------------------
        // ota_start
        //--------------------------------------------------

        val startAck = CompletableDeferred<Boolean>()
        otaStartAck = startAck

        send("{\"cmd\":\"ota_start\",\"size\":$totalSize}")

        val started = withTimeoutOrNull(5000) { startAck.await() } ?: false

        if (!started) {
            Log.e("TEST", "performOtaUpdate() - ota_start not acknowledged")
            otaStartAck = null
            _otaState.value = OtaState.FAILED
            return false
        }

        //--------------------------------------------------
        // Stream firmware in chunks
        //--------------------------------------------------

        _otaState.value = OtaState.TRANSFERRING

        val chunkSize = conn.getMtuPayloadSize()
        var offset = 0

        while (offset < totalSize) {

            val end = (offset + chunkSize).coerceAtMost(totalSize)
            val chunk = firmware.copyOfRange(offset, end)

            val ok = conn.writeOtaChunk(chunk)

            if (!ok) {
                Log.e("TEST", "performOtaUpdate() - chunk write failed at offset $offset")
                send("{\"cmd\":\"ota_abort\"}")
                _otaState.value = OtaState.FAILED
                return false
            }

            offset = end
            _otaProgress.value = offset to totalSize
        }

        //--------------------------------------------------
        // ota_end
        //--------------------------------------------------

        _otaState.value = OtaState.FINISHING

        val endAck = CompletableDeferred<Boolean>()
        otaEndAck = endAck

        send("{\"cmd\":\"ota_end\"}")

        // Longer timeout than ota_start - the ESP32 is verifying and
        // finalizing the whole image on flash, which takes a few
        // seconds longer than a simple ack.
        val completed = withTimeoutOrNull(20_000) { endAck.await() } ?: false

        otaEndAck = null

        _otaState.value = if (completed) OtaState.SUCCESS else OtaState.FAILED

        if (completed) {
            Log.e("TEST", "performOtaUpdate() - complete, device should now be rebooting")
        } else {
            Log.e("TEST", "performOtaUpdate() - ota_end not acknowledged")
        }

        return completed
    }

    fun resetOtaState() {
        _otaState.value = OtaState.IDLE
        _otaProgress.value = 0 to 0
    }
}