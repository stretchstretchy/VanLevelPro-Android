package com.vanlevelpro.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vanlevelpro.app.bluetooth.BluetoothManager
import com.vanlevelpro.app.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val bluetoothManager =
        BluetoothManager(application)

    private val settingsRepository =
        SettingsRepository(application)

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

    val connectionState =
        bluetoothManager.connectionState.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            bluetoothManager.connectionState.value
        )

    fun scan() {
        bluetoothManager.scan()
    }

    fun disconnect() {
        bluetoothManager.disconnect()
    }

    fun calibrate() {
        bluetoothManager.calibrate()
    }

    fun forgetDevice() {
        bluetoothManager.forgetRememberedDevice()
    }

    //--------------------------------------------------
    // Level thresholds (Settings screen)
    //--------------------------------------------------

    val greenThreshold: StateFlow<Float> =
        settingsRepository.greenThreshold

    val yellowThreshold: StateFlow<Float> =
        settingsRepository.yellowThreshold

    fun setGreenThreshold(value: Float) {
        settingsRepository.setGreenThreshold(value)
    }

    fun setYellowThreshold(value: Float) {
        settingsRepository.setYellowThreshold(value)
    }

    fun resetThresholdsToDefaults() {
        settingsRepository.resetToDefaults()
    }
}