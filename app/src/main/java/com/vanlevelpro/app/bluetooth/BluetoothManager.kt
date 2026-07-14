package com.vanlevelpro.app.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
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

    //--------------------------------------------------
    // State
    //--------------------------------------------------

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

        val bluetoothAdapter = adapter

        if (bluetoothAdapter == null) {
            _status.value = "Bluetooth not supported"
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            _status.value = "Bluetooth is OFF"
            return
        }

        scanner?.stop()

        _connectionState.value = ConnectionState.SCANNING

        scanner = BleScanner(

            adapter = bluetoothAdapter,

            onDeviceFound = { device: BluetoothDevice ->

                scanner?.stop()

                connect(device)
            },

            onStatus = { message ->

                _status.value = message
            }
        )

        scanner?.start()
    }

    //--------------------------------------------------
    // Connect
    //--------------------------------------------------

    private fun connect(device: BluetoothDevice) {

        _connectionState.value = ConnectionState.CONNECTING

        connection?.disconnect()

        connection = BleConnection(

            context = context,

            device = device,

            onStatus = { message ->

                _status.value = message

                if (message == "Connected") {
                    _connectionState.value = ConnectionState.CONNECTED
                }
            },

            onTelemetry = { json ->

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

                roll = obj.optDouble("roll", 0.0).toFloat(),

                connected = true
            )

        } catch (e: Exception) {

            _status.value = json
        }
    }

    //--------------------------------------------------
    // Disconnect
    //--------------------------------------------------

    fun disconnect() {

        scanner?.stop()

        connection?.disconnect()

        _connectionState.value = ConnectionState.DISCONNECTED

        _telemetry.value = Telemetry()

        _status.value = "Disconnected"
    }

    //--------------------------------------------------
    // Send
    //--------------------------------------------------

    fun send(json: String) {

        // Next milestone
    }
}