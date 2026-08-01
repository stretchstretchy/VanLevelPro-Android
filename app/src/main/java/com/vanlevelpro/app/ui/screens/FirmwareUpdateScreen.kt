package com.vanlevelpro.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vanlevelpro.app.bluetooth.BluetoothManager
import com.vanlevelpro.app.model.ConnectionState
import com.vanlevelpro.app.viewmodel.MainViewModel

@Composable
fun FirmwareUpdateScreen(
    viewModel: MainViewModel
) {

    val connectionState by viewModel.connectionState.collectAsState()

    val deviceFirmwareVersion by viewModel.deviceFirmwareVersion.collectAsState()

    val updateStatus by viewModel.firmwareUpdateStatus.collectAsState()

    val availableUpdate by viewModel.availableFirmwareUpdate.collectAsState()

    val otaState by viewModel.otaState.collectAsState()

    val otaProgress by viewModel.otaProgress.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {

        Text(
            "Firmware Update",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        if (connectionState != ConnectionState.CONNECTED) {

            Text(
                text = "Connect to your VanLevel Pro unit first to check " +
                        "for or install a firmware update.",
                color = Color(0xFFE53935),
                style = MaterialTheme.typography.bodyMedium
            )

            return@Column
        }

        Text(
            text = "Device version: ${deviceFirmwareVersion ?: "Unknown"}",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(24.dp))

        HorizontalDivider()

        Spacer(Modifier.height(24.dp))

        when (updateStatus) {

            MainViewModel.FirmwareUpdateStatus.IDLE -> {
                OutlinedButton(
                    onClick = { viewModel.checkForFirmwareUpdate() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Check for Firmware Update")
                }
            }

            MainViewModel.FirmwareUpdateStatus.CHECKING_VERSION,
            MainViewModel.FirmwareUpdateStatus.CHECKING -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
                    Text(
                        if (updateStatus == MainViewModel.FirmwareUpdateStatus.CHECKING_VERSION)
                            "Reading device version..."
                        else
                            "Checking for updates..."
                    )
                }
            }

            MainViewModel.FirmwareUpdateStatus.UP_TO_DATE -> {
                Text(
                    "Firmware is up to date.",
                    color = Color(0xFF4CAF50)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.checkForFirmwareUpdate() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Check Again")
                }
            }

            MainViewModel.FirmwareUpdateStatus.AVAILABLE -> {

                Text(
                    text = "Update available: ${availableUpdate?.versionTag ?: ""}",
                    color = Color(0xFFFFB300),
                    fontWeight = FontWeight.Bold
                )

                if (!availableUpdate?.releaseNotes.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = availableUpdate?.releaseNotes.orEmpty(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Keep the app open and the device powered during " +
                            "the update - it will disconnect and restart on " +
                            "its own once complete.",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.installFirmwareUpdate() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Now")
                }
            }

            MainViewModel.FirmwareUpdateStatus.DOWNLOADING -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
                    Text("Downloading firmware...")
                }
            }

            MainViewModel.FirmwareUpdateStatus.INSTALLING -> {

                val (sent, total) = otaProgress

                val label = when (otaState) {
                    BluetoothManager.OtaState.STARTING -> "Starting update..."
                    BluetoothManager.OtaState.TRANSFERRING -> "Sending firmware to device..."
                    BluetoothManager.OtaState.FINISHING -> "Verifying and finishing up..."
                    else -> "Installing..."
                }

                Text(label, fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(12.dp))

                if (total > 0) {

                    LinearProgressIndicator(
                        progress = { sent.toFloat() / total.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "${sent / 1024} KB / ${total / 1024} KB",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Do not close the app, disconnect the device, " +
                            "or lose power during this process.",
                    color = Color(0xFFE53935),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            MainViewModel.FirmwareUpdateStatus.SUCCESS -> {

                Text(
                    "Update complete!",
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "The device is restarting with the new firmware " +
                            "and will reconnect automatically.",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { viewModel.resetFirmwareUpdateState() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }

            MainViewModel.FirmwareUpdateStatus.FAILED -> {

                Text(
                    "Update failed.",
                    color = Color(0xFFE53935),
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "The device's currently running firmware is " +
                            "untouched and unaffected - it's safe to try again.",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { viewModel.resetFirmwareUpdateState() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Try Again")
                }
            }
        }
    }
}