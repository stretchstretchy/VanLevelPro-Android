package com.vanlevelpro.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log
import com.vanlevelpro.app.protocol.BleUuids

class BleScanner(
    private val adapter: BluetoothAdapter,
    private val onDeviceFound: (BluetoothDevice) -> Unit,
    private val onStatus: (String) -> Unit
) {

    companion object {
        private const val TAG = "VanLevelScanner"
    }

    private val scanner: BluetoothLeScanner?
        get() = adapter.bluetoothLeScanner

    private var foundDevice = false

    private val filters = listOf(
        ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleUuids.SERVICE_UUID))
            .build()
    )

    private val settings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val callback = object : ScanCallback() {

        @SuppressLint("MissingPermission")
        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {

            if (foundDevice) return

            val device = result.device

            val name =
                result.scanRecord?.deviceName
                    ?: device.name
                    ?: "Unknown"

            Log.d(TAG, "Found device: $name (${device.address}) RSSI=${result.rssi}")

            if (name != BluetoothManager.DEVICE_NAME) {
                return
            }

            foundDevice = true

            onStatus("Found: $name")

            stop()

            Log.d(TAG, "Connecting to ${device.address}")

            onDeviceFound(device)
        }

        override fun onScanFailed(errorCode: Int) {

            Log.e(TAG, "Scan failed: $errorCode")

            foundDevice = false

            onStatus("Scan Failed ($errorCode)")
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {

        foundDevice = false

        onStatus("Starting BLE Scan...")

        Log.d(TAG, "Starting BLE scan")

        scanner?.startScan(
            filters,
            settings,
            callback
        )
    }

    @SuppressLint("MissingPermission")
    fun stop() {

        Log.d(TAG, "Stopping BLE scan")

        scanner?.stopScan(callback)
    }
}