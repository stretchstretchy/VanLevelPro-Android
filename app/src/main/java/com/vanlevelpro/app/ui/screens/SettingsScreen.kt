package com.vanlevelpro.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vanlevelpro.app.settings.SettingsRepository
import com.vanlevelpro.app.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel
) {

    val greenThreshold by viewModel.greenThreshold.collectAsState()

    val yellowThreshold by viewModel.yellowThreshold.collectAsState()

    val updateStatus by viewModel.updateStatus.collectAsState()

    val availableUpdate by viewModel.availableUpdate.collectAsState()

    var showForgetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {

        Text(
            "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Adjust how many degrees off level the gauges " +
                    "tolerate before turning yellow, and then red.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(32.dp))

        // -------------------------------------------------
        // Green threshold
        // -------------------------------------------------

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                Box(
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        "●",
                        color = Color(0xFF4CAF50),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Text(
                    "Level (Green) Threshold",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = "${"%.1f".format(greenThreshold)}°",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = greenThreshold,
            onValueChange = { viewModel.setGreenThreshold(it) },
            valueRange = SettingsRepository.MIN_GREEN_THRESHOLD..yellowThreshold
        )

        Spacer(Modifier.height(24.dp))

        // -------------------------------------------------
        // Yellow threshold
        // -------------------------------------------------

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                Box(
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        "●",
                        color = Color(0xFFFFB300),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Text(
                    "Warning (Yellow) Threshold",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = "${"%.1f".format(yellowThreshold)}°",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = yellowThreshold,
            onValueChange = { viewModel.setYellowThreshold(it) },
            valueRange = greenThreshold..SettingsRepository.MAX_YELLOW_THRESHOLD
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Above ${"%.1f".format(yellowThreshold)}° the gauges show red.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(32.dp))

        HorizontalDivider()

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = { viewModel.resetThresholdsToDefaults() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset to Defaults (0.5° / 2.0°)")
        }

        Spacer(Modifier.height(32.dp))

        HorizontalDivider()

        Spacer(Modifier.height(24.dp))

        Text(
            "Connection",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "The app remembers your VanLevel Pro unit and " +
                    "reconnects to it directly each time. Forget it if " +
                    "you're switching to a different unit (e.g. testing " +
                    "someone else's board).",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { showForgetDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE53935)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Forget This Device")
        }

        Spacer(Modifier.height(32.dp))

        HorizontalDivider()

        Spacer(Modifier.height(24.dp))

        Text(
            "Updates",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Current version: ${com.vanlevelpro.app.BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(16.dp))

        when (updateStatus) {

            MainViewModel.UpdateStatus.IDLE -> {
                OutlinedButton(
                    onClick = { viewModel.checkForUpdates() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Check for Updates")
                }
            }

            MainViewModel.UpdateStatus.CHECKING -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
                    Text("Checking for updates...")
                }
            }

            MainViewModel.UpdateStatus.UP_TO_DATE -> {
                Text(
                    "You're on the latest version.",
                    color = Color(0xFF4CAF50)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.checkForUpdates() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Check Again")
                }
            }

            MainViewModel.UpdateStatus.AVAILABLE -> {

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

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.downloadUpdate() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Download Update")
                }
            }

            MainViewModel.UpdateStatus.DOWNLOADING -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
                    Text("Downloading update...")
                }
            }

            MainViewModel.UpdateStatus.READY_TO_INSTALL -> {

                Text(
                    "Update downloaded.",
                    color = Color(0xFF4CAF50)
                )

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.installUpdate() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Install Update")
                }
            }

            MainViewModel.UpdateStatus.FAILED -> {
                Text(
                    "Update check failed - check your internet connection.",
                    color = Color(0xFFE53935)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.checkForUpdates() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Try Again")
                }
            }
        }
    }

    if (showForgetDialog) {

        AlertDialog(
            onDismissRequest = { showForgetDialog = false },
            title = { Text("Forget this device?") },
            text = {
                Text(
                    "The app will search for any nearby VanLevel Pro " +
                            "unit next time you connect, instead of " +
                            "reconnecting directly to this one."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.forgetDevice()
                        showForgetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Text("Forget")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showForgetDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}