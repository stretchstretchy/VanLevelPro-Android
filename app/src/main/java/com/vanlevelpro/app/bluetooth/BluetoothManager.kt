package com.vanlevelpro.app.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import com.vanlevelpro.app.model.ConnectionState
import com.vanlevelpro.app.model.Telemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class BluetoothManager(private val context: Context) {

    companion object {
        const val DEVICE_NAME = "VanLevel Pro"
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

                    directConnectTimeoutRunnable?.let {
                        directConnectHandler.removeCallbacks(it)
                    }
                    directConnectTimeoutRunnable = null

                    lastKnownAddress = device.address
                }
            },

            onTelemetry = { json ->

                Log.e("TEST", "Telemetry: $json")

                _status.value = json
                parseTelemetry(json)
            }
        )

        connection?.connect()
    }

    //--------------------------------------------------
    // Parse JSON
    //--------------------------------------------------

    private fun parseTelemetry(json: String) {

        try {

            val obj = JSONObject(json)

            if (obj.optString("type") != "telemetry")
                return

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

        } catch (e: Exception) {

            Log.e("TEST", "JSON ERROR", e)

            _status.value = json
        }
    }

    //--------------------------------------------------
    // Disconnect
    //--------------------------------------------------

    fun disconnect() {

        Log.e("TEST", "Disconnect()")

        stopHeartbeat()

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
}