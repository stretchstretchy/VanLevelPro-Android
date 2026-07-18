package com.vanlevelpro.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import com.vanlevelpro.app.protocol.BleUuids
import java.util.UUID

class BleConnection(
    private val context: Context,
    private val device: BluetoothDevice,
    private val onStatus: (String) -> Unit,
    private val onTelemetry: (String) -> Unit
) {

    companion object {

        private const val TAG = "TEST"

        private val CCCD_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private var gatt: BluetoothGatt? = null

    private var disconnectRequested = false

    private var commandCharacteristic: BluetoothGattCharacteristic? = null

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    fun writeCommand(json: String): Boolean {

        val characteristic = commandCharacteristic
        val g = gatt

        if (characteristic == null) {
            Log.e(TAG, "writeCommand() aborted - commandCharacteristic is null")
            return false
        }

        if (g == null) {
            Log.e(TAG, "writeCommand() aborted - gatt is null")
            return false
        }

        val data = json.toByteArray(Charsets.UTF_8)

        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val status = g.writeCharacteristic(
                    characteristic,
                    data,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
                Log.e(TAG, "writeCommand() status=$status")
                status == android.bluetooth.BluetoothStatusCodes.SUCCESS
            } else {
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                characteristic.value = data
                val ok = g.writeCharacteristic(characteristic)
                Log.e(TAG, "writeCommand() legacy result=$ok")
                ok
            }
        } catch (e: Exception) {
            Log.e(TAG, "writeCommand() failed", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun connect() {

        Log.e(TAG, "connectGatt()")

        disconnectRequested = false

        onStatus("Connecting...")

        gatt = device.connectGatt(
            context,
            false,
            callback
        )
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {

        Log.e(TAG, "disconnect() called")

        disconnectRequested = true

        gatt?.disconnect()
        // IMPORTANT:
        // DO NOT close() here.
        // Wait until STATE_DISCONNECTED.
    }

    private val callback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {

            Log.e(
                TAG,
                "onConnectionStateChange status=$status newState=$newState"
            )

            when (newState) {

                BluetoothProfile.STATE_CONNECTED -> {

                    onStatus("Connected")

                    Log.e(TAG, "Requesting MTU")

                    gatt.requestMtu(247)
                }

                BluetoothProfile.STATE_DISCONNECTED -> {

                    Log.e(TAG, "STATE_DISCONNECTED")

                    onStatus("Disconnected")

                    try {
                        gatt.close()
                        Log.e(TAG, "Gatt closed")
                    } catch (e: Exception) {
                        Log.e(TAG, "close() failed", e)
                    }

                    if (this@BleConnection.gatt === gatt) {
                        this@BleConnection.gatt = null
                    }

                    commandCharacteristic = null
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(
            gatt: BluetoothGatt,
            mtu: Int,
            status: Int
        ) {

            Log.e(TAG, "MTU=$mtu")

            gatt.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(
            gatt: BluetoothGatt,
            status: Int
        ) {

            Log.e(TAG, "Services discovered status=$status")

            if (status != BluetoothGatt.GATT_SUCCESS) {

                onStatus("Service Discovery Failed")
                return
            }

            val service = gatt.getService(BleUuids.SERVICE_UUID)

            if (service == null) {

                Log.e(TAG, "Service missing")

                onStatus("VanLevel Service Missing")
                return
            }

            val telemetryCharacteristic =
                service.getCharacteristic(BleUuids.TELEMETRY_UUID)

            if (telemetryCharacteristic == null) {

                Log.e(TAG, "Telemetry characteristic missing")

                onStatus("Telemetry Characteristic Missing")
                return
            }

            commandCharacteristic =
                service.getCharacteristic(BleUuids.COMMAND_UUID)

            if (commandCharacteristic == null) {
                Log.e(TAG, "Command characteristic missing (heartbeat pings will not work)")
            }

            val descriptor =
                telemetryCharacteristic.getDescriptor(CCCD_UUID)

            if (descriptor == null) {

                Log.e(TAG, "CCCD missing")

                onStatus("CCCD Missing")
                return
            }

            Log.e(TAG, "Enabling notifications")

            gatt.setCharacteristicNotification(
                telemetryCharacteristic,
                true
            )

            descriptor.value =
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

            gatt.writeDescriptor(descriptor)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {

            Log.e(TAG, "Descriptor write status=$status")

            if (status == BluetoothGatt.GATT_SUCCESS) {
                onStatus("Notifications Enabled")
            } else {
                onStatus("Failed To Enable Notifications")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {

            val json = String(value, Charsets.UTF_8)

            Log.e(TAG, "RX: $json")

            onTelemetry(json)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {

            val value = characteristic.value ?: return

            val json = String(value, Charsets.UTF_8)

            Log.e(TAG, "RX: $json")

            onTelemetry(json)
        }
    }
}