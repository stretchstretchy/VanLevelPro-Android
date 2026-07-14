package com.vanlevelpro.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vanlevelpro.app.bluetooth.BluetoothManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val bluetoothManager =
        BluetoothManager(application)

    val status =
        bluetoothManager.status.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            bluetoothManager.status.value
        )

    val telemetry =
        bluetoothManager.telemetry.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            bluetoothManager.telemetry.value
        )

    fun scan() {
        bluetoothManager.scan()
    }

    fun disconnect() {
        bluetoothManager.disconnect()
    }
}