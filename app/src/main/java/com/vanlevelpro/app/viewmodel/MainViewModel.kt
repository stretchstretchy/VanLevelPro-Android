package com.vanlevelpro.app.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vanlevelpro.app.bluetooth.BluetoothManager
import com.vanlevelpro.app.settings.SettingsRepository
import com.vanlevelpro.app.update.UpdateChecker
import com.vanlevelpro.app.update.UpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    // AndroidViewModel already has its own PRIVATE 'application' field
    // internally, which shadows/conflicts with our constructor
    // parameter of the same name once you're outside a property
    // initializer (e.g. inside onCleared(), or any regular function
    // below) - alias it to our own clearly-named property to avoid
    // that ambiguity everywhere in this class.
    private val appContext: Application = application

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

    //--------------------------------------------------
    // App updates (GitHub Releases)
    //--------------------------------------------------

    enum class UpdateStatus {
        IDLE, CHECKING, UP_TO_DATE, AVAILABLE, DOWNLOADING, READY_TO_INSTALL, FAILED
    }

    private val _updateStatus =
        MutableStateFlow(UpdateStatus.IDLE)

    val updateStatus: StateFlow<UpdateStatus> =
        _updateStatus.asStateFlow()

    private val _availableUpdate =
        MutableStateFlow<UpdateInfo?>(null)

    val availableUpdate: StateFlow<UpdateInfo?> =
        _availableUpdate.asStateFlow()

    private var activeDownloadId: Long? = null

    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            if (id == activeDownloadId) {
                _updateStatus.value = UpdateStatus.READY_TO_INSTALL
            }
        }
    }

    init {
        ContextCompat.registerReceiver(
            appContext,
            downloadCompleteReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onCleared() {
        super.onCleared()
        appContext.unregisterReceiver(downloadCompleteReceiver)
    }

    fun checkForUpdates() {

        _updateStatus.value = UpdateStatus.CHECKING

        viewModelScope.launch {

            val result = UpdateChecker.checkForUpdate()

            if (result != null) {
                _availableUpdate.value = result
                _updateStatus.value = UpdateStatus.AVAILABLE
            } else {
                _availableUpdate.value = null
                _updateStatus.value = UpdateStatus.UP_TO_DATE
            }
        }
    }

    fun downloadUpdate() {

        val update = _availableUpdate.value ?: return

        _updateStatus.value = UpdateStatus.DOWNLOADING

        activeDownloadId = UpdateChecker.downloadUpdate(appContext, update.apkDownloadUrl)
    }

    fun installUpdate() {
        UpdateChecker.installApk(appContext)
    }
}