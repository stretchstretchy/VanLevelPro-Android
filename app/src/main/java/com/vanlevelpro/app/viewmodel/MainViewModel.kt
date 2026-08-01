package com.vanlevelpro.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vanlevelpro.app.bluetooth.BluetoothManager
import com.vanlevelpro.app.model.ConnectionState
import com.vanlevelpro.app.settings.SettingsRepository
import com.vanlevelpro.app.update.FirmwareUpdateChecker
import com.vanlevelpro.app.update.FirmwareUpdateInfo
import com.vanlevelpro.app.update.UpdateChecker
import com.vanlevelpro.app.update.UpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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

    val deviceFirmwareVersion =
        bluetoothManager.deviceFirmwareVersion.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            bluetoothManager.deviceFirmwareVersion.value
        )

    fun requestFirmwareVersion() {
        bluetoothManager.requestFirmwareVersion()
    }

    val otaState =
        bluetoothManager.otaState.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            bluetoothManager.otaState.value
        )

    val otaProgress =
        bluetoothManager.otaProgress.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            bluetoothManager.otaProgress.value
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

    fun checkForUpdates() {

        _updateStatus.value = UpdateStatus.CHECKING

        viewModelScope.launch {

            when (val result = UpdateChecker.checkForUpdate()) {

                is UpdateChecker.CheckResult.UpdateAvailable -> {
                    _availableUpdate.value = result.info
                    _updateStatus.value = UpdateStatus.AVAILABLE
                }

                is UpdateChecker.CheckResult.UpToDate -> {
                    _availableUpdate.value = null
                    _updateStatus.value = UpdateStatus.UP_TO_DATE
                }

                is UpdateChecker.CheckResult.CheckFailed -> {
                    _availableUpdate.value = null
                    _updateStatus.value = UpdateStatus.FAILED
                }
            }
        }
    }

    fun downloadUpdate() {

        val update = _availableUpdate.value ?: return

        _updateStatus.value = UpdateStatus.DOWNLOADING

        val id = UpdateChecker.downloadUpdate(appContext, update.apkDownloadUrl)
        activeDownloadId = id

        viewModelScope.launch {

            val success = UpdateChecker.awaitDownloadCompletion(appContext, id)

            _updateStatus.value =
                if (success) UpdateStatus.READY_TO_INSTALL else UpdateStatus.FAILED
        }
    }

    fun installUpdate() {
        UpdateChecker.installApk(appContext)
    }

    //--------------------------------------------------
    // Firmware updates (ESP32, over BLE)
    //--------------------------------------------------

    enum class FirmwareUpdateStatus {
        IDLE, CHECKING_VERSION, CHECKING, UP_TO_DATE, AVAILABLE,
        DOWNLOADING, INSTALLING, SUCCESS, FAILED
    }

    private val _firmwareUpdateStatus =
        MutableStateFlow(FirmwareUpdateStatus.IDLE)

    val firmwareUpdateStatus: StateFlow<FirmwareUpdateStatus> =
        _firmwareUpdateStatus.asStateFlow()

    private val _availableFirmwareUpdate =
        MutableStateFlow<FirmwareUpdateInfo?>(null)

    val availableFirmwareUpdate: StateFlow<FirmwareUpdateInfo?> =
        _availableFirmwareUpdate.asStateFlow()

    fun checkForFirmwareUpdate() {

        if (connectionState.value != ConnectionState.CONNECTED) {
            _firmwareUpdateStatus.value = FirmwareUpdateStatus.FAILED
            return
        }

        _firmwareUpdateStatus.value = FirmwareUpdateStatus.CHECKING_VERSION

        viewModelScope.launch {

            requestFirmwareVersion()

            // Wait for the device to actually reply with its version -
            // it may have already reported one recently (in which case
            // this returns immediately with the existing value only if
            // it changes; since it's the same request/response pair
            // each time, briefly clear it first so we know we're
            // seeing a fresh reply rather than a stale one).
            val version = withTimeoutOrNull(5000) {
                deviceFirmwareVersion.filterNotNull().first()
            }

            if (version == null) {
                Log.e("TEST", "checkForFirmwareUpdate() - device did not report a version")
                _firmwareUpdateStatus.value = FirmwareUpdateStatus.FAILED
                return@launch
            }

            _firmwareUpdateStatus.value = FirmwareUpdateStatus.CHECKING

            when (val result = FirmwareUpdateChecker.checkForUpdate(version)) {

                is FirmwareUpdateChecker.CheckResult.UpdateAvailable -> {
                    _availableFirmwareUpdate.value = result.info
                    _firmwareUpdateStatus.value = FirmwareUpdateStatus.AVAILABLE
                }

                is FirmwareUpdateChecker.CheckResult.UpToDate -> {
                    _availableFirmwareUpdate.value = null
                    _firmwareUpdateStatus.value = FirmwareUpdateStatus.UP_TO_DATE
                }

                is FirmwareUpdateChecker.CheckResult.CheckFailed -> {
                    _availableFirmwareUpdate.value = null
                    _firmwareUpdateStatus.value = FirmwareUpdateStatus.FAILED
                }
            }
        }
    }

    fun installFirmwareUpdate() {

        val update = _availableFirmwareUpdate.value ?: return

        _firmwareUpdateStatus.value = FirmwareUpdateStatus.DOWNLOADING

        viewModelScope.launch {

            val firmwareBytes = FirmwareUpdateChecker.downloadFirmware(update.binDownloadUrl)

            if (firmwareBytes == null) {
                Log.e("TEST", "installFirmwareUpdate() - download failed")
                _firmwareUpdateStatus.value = FirmwareUpdateStatus.FAILED
                return@launch
            }

            _firmwareUpdateStatus.value = FirmwareUpdateStatus.INSTALLING

            // Detailed progress during the actual BLE transfer is
            // exposed separately via otaState/otaProgress - the UI
            // reads those directly while this is FirmwareUpdateStatus.INSTALLING.
            val success = bluetoothManager.performOtaUpdate(firmwareBytes)

            _firmwareUpdateStatus.value =
                if (success) FirmwareUpdateStatus.SUCCESS else FirmwareUpdateStatus.FAILED
        }
    }

    fun resetFirmwareUpdateState() {
        _firmwareUpdateStatus.value = FirmwareUpdateStatus.IDLE
        _availableFirmwareUpdate.value = null
        bluetoothManager.resetOtaState()
    }
}