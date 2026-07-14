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
        private const val TAG = "VanLevelBLE"

        private val CCCD_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private var gatt: BluetoothGatt? = null

    @SuppressLint("MissingPermission")
    fun connect() {

        onStatus("Connecting...")

        gatt = device.connectGatt(
            context,
            false,
            callback
        )
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {

        gatt?.disconnect()
        gatt?.close()
        gatt = null
    }

    private val callback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {

            when (newState) {

                BluetoothProfile.STATE_CONNECTED -> {

                    onStatus("Connected")

                    Log.d(TAG, "Requesting MTU 247")

                    gatt.requestMtu(247)
                }

                BluetoothProfile.STATE_DISCONNECTED -> {

                    onStatus("Disconnected")
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(
            gatt: BluetoothGatt,
            mtu: Int,
            status: Int
        ) {

            Log.d(TAG, "MTU = $mtu")

            onStatus("MTU $mtu")

            gatt.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(
            gatt: BluetoothGatt,
            status: Int
        ) {

            if (status != BluetoothGatt.GATT_SUCCESS) {

                onStatus("Service Discovery Failed")
                return
            }

            val service =
                gatt.getService(BleUuids.SERVICE_UUID)

            if (service == null) {

                onStatus("VanLevel Service Missing")
                return
            }

            val telemetryCharacteristic =
                service.getCharacteristic(BleUuids.TELEMETRY_UUID)

            if (telemetryCharacteristic == null) {

                onStatus("Telemetry Characteristic Missing")
                return
            }

            val descriptor =
                telemetryCharacteristic.getDescriptor(CCCD_UUID)

            if (descriptor == null) {

                onStatus("CCCD Missing")
                return
            }

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

            Log.d(TAG, "RX (${value.size} bytes): $json")

            onTelemetry(json)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {

            val value = characteristic.value ?: return

            val json = String(value, Charsets.UTF_8)

            Log.d(TAG, "RX (${value.size} bytes): $json")

            onTelemetry(json)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            // Not used
        }
    }
}