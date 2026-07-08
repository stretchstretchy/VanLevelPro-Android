package com.vanlevelpro.app.viewmodel

import android.app.Application
import android.bluetooth.BluetoothManager
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(application: Application) : AndroidViewModel(application)
{
    private val bluetoothManager =
        application.getSystemService(BluetoothManager::class.java)

    private val _status = MutableStateFlow("Checking Bluetooth...")

    val status: StateFlow<String> = _status.asStateFlow()

    init
    {
        val adapter = bluetoothManager.adapter

        _status.value =
            if (adapter == null)
                "Bluetooth Not Supported"
            else if (!adapter.isEnabled)
                "Bluetooth Off"
            else
                "Bluetooth Ready"
    }
}