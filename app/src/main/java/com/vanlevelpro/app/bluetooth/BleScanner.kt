package com.vanlevelpro.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.BluetoothLeScanner

class BleScanner(
    private val adapter: BluetoothAdapter,
    private val onDeviceFound: (String) -> Unit
) {

    private val scanner: BluetoothLeScanner?
        get() = adapter.bluetoothLeScanner

    private val callback = object : ScanCallback() {

        @SuppressLint("MissingPermission")
        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {

            val name = result.device.name ?: return

            if (name == "VanLevel Pro") {
                onDeviceFound(name)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        scanner?.startScan(callback)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        scanner?.stopScan(callback)
    }
}