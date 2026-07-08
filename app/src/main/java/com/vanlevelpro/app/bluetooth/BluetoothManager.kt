package com.vanlevelpro.app.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.vanlevelpro.app.model.ConnectionState
import com.vanlevelpro.app.model.Telemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BluetoothManager(private val context: Context) {

    companion object {
        const val DEVICE_NAME = "VanLevel Pro"
    }

    //--------------------------------------------------
    // Android Bluetooth
    //--------------------------------------------------

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val adapter: BluetoothAdapter?
        get() = bluetoothManager.adapter

    //--------------------------------------------------
    // Scanner
    //--------------------------------------------------

    private var scanner: BleScanner? = null

    //--------------------------------------------------
    // State
    //--------------------------------------------------

    private val _connectionState =
        MutableStateFlow(ConnectionState.DISCONNECTED)

    val connectionState: StateFlow<ConnectionState> =
        _connectionState.asStateFlow()

    private val _status =
        MutableStateFlow("Idle")

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

        if (adapter == null) {
            _status.value = "Bluetooth not supported"
            return
        }

        if (!adapter!!.isEnabled) {
            _status.value = "Bluetooth is OFF"
            return
        }

        _connectionState.value = ConnectionState.SCANNING
        _status.value = "Scanning..."

        scanner = BleScanner(adapter!!) { deviceName ->

            _status.value = "Found: $deviceName"

            scanner?.stop()
        }

        scanner?.start()
    }

    //--------------------------------------------------
    // Connect
    //--------------------------------------------------

    fun connect() {

        _connectionState.value = ConnectionState.CONNECTING
        _status.value = "Connecting..."
    }

    //--------------------------------------------------
    // Disconnect
    //--------------------------------------------------

    fun disconnect() {

        scanner?.stop()

        _connectionState.value = ConnectionState.DISCONNECTED
        _status.value = "Disconnected"
    }

    //--------------------------------------------------
    // Send
    //--------------------------------------------------

    fun send(json: String) {
        // Next milestone
    }
}