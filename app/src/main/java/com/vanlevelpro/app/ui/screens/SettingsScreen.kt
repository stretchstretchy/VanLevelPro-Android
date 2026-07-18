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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    Column(
        modifier = Modifier
            .fillMaxSize()
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
    }
}
