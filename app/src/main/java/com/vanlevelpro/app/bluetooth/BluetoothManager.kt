package com.vanlevelpro.app.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.vanlevelpro.app.model.ConnectionState
import com.vanlevelpro.app.model.Telemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BluetoothManager(private val context: Context)
{
    companion object
    {
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
    // Connection State
    //--------------------------------------------------

    private val _connectionState =
        MutableStateFlow(ConnectionState.DISCONNECTED)

    val connectionState: StateFlow<ConnectionState> =
        _connectionState.asStateFlow()

    //--------------------------------------------------
    // Telemetry
    //--------------------------------------------------

    private val _telemetry =
        MutableStateFlow(Telemetry())

    val telemetry: StateFlow<Telemetry> =
        _telemetry.asStateFlow()

    //--------------------------------------------------
    // Public Functions
    //--------------------------------------------------

    fun scan()
    {
        _connectionState.value = ConnectionState.SCANNING
    }

    fun connect()
    {
        _connectionState.value = ConnectionState.CONNECTING
    }

    fun disconnect()
    {
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun send(json: String)
    {
        // BLE write coming next
    }
}