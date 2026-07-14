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
import com.vanlevelpro.app.protocol.BleUuids

class BleScanner(
    private val adapter: BluetoothAdapter,
    private val onDeviceFound: (BluetoothDevice) -> Unit,
    private val onStatus: (String) -> Unit
) {

    private val scanner: BluetoothLeScanner?
        get() = adapter.bluetoothLeScanner

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

            val device = result.device

            val name =
                result.scanRecord?.deviceName
                    ?: device.name
                    ?: "Unknown"

            onStatus(
                "Found: $name\nRSSI ${result.rssi}"
            )

            onDeviceFound(device)
        }

        override fun onScanFailed(errorCode: Int) {

            onStatus("Scan Failed ($errorCode)")
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {

        onStatus("Starting BLE Scan...")

        scanner?.startScan(
            filters,
            settings,
            callback
        )
    }

    @SuppressLint("MissingPermission")
    fun stop() {

        scanner?.stopScan(callback)
    }
}