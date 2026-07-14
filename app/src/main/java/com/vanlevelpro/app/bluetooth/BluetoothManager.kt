package com.vanlevelpro.app.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
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

    private val systemBluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager

    private val adapter: BluetoothAdapter?
        get() = systemBluetoothManager.adapter

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

        _connectionState.value = ConnectionState.SCANNING

        scanner?.stop()

        scanner = BleScanner(

            adapter = bluetoothAdapter,

            onDeviceFound = { device: BluetoothDevice ->

                scanner?.stop()

                _connectionState.value = ConnectionState.CONNECTING

                _status.value =
                    "Found:\n${device.name ?: "VanLevel Pro"}"
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