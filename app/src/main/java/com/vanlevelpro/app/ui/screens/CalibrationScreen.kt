package com.vanlevelpro.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vanlevelpro.app.viewmodel.MainViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    viewModel: MainViewModel
) {

    val telemetry by viewModel.telemetry.collectAsState()

    val greenThreshold by viewModel.greenThreshold.collectAsState()

    val level =
        abs(telemetry.pitch) < greenThreshold &&
                abs(telemetry.roll) < greenThreshold

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            "Calibration",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Place a spirit level on the van, use your levelling " +
                    "system to get it level in both directions, then press " +
                    "Calibrate to zero the sensor.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // -------------------------------------------------
        // Live readings
        // -------------------------------------------------

        Text(
            text = "Pitch: ${"%.1f".format(telemetry.pitch)}°",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Roll: ${"%.1f".format(telemetry.roll)}°",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = if (level) "LEVEL" else "NOT LEVEL",
            color =
                if (level)
                    Color(0xFF4CAF50)
                else
                    Color(0xFFFFB300),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(32.dp))

        // -------------------------------------------------
        // Calibration status
        // -------------------------------------------------

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            Icon(
                imageVector =
                    if (telemetry.calibrated)
                        Icons.Default.CheckCircle
                    else
                        Icons.Default.Warning,
                contentDescription = null,
                tint =
                    if (telemetry.calibrated)
                        Color(0xFF4CAF50)
                    else
                        Color(0xFFFFB300)
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text =
                    if (telemetry.calibrated)
                        "Sensor is calibrated"
                    else
                        "Sensor is not calibrated",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(Modifier.height(32.dp))

        // -------------------------------------------------
        // Calibrate button
        // -------------------------------------------------

        Button(
            onClick = {
                viewModel.calibrate()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calibrate Now")
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "This sets the current position as the new zero point " +
                    "and is stored on the device, surviving power cycles.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}