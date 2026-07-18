package com.vanlevelpro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vanlevelpro.app.model.ConnectionState
import com.vanlevelpro.app.viewmodel.MainViewModel

@Composable
fun DiagnosticsScreen(
    viewModel: MainViewModel
) {

    val connectionState by viewModel.connectionState.collectAsState()

    val telemetry by viewModel.telemetry.collectAsState()

    val (statusLabel, statusColor) = when (connectionState) {
        ConnectionState.CONNECTED -> "Connected" to Color(0xFF4CAF50)
        ConnectionState.CONNECTING -> "Connecting..." to Color(0xFFFFB300)
        ConnectionState.SCANNING -> "Scanning..." to Color(0xFFFFB300)
        ConnectionState.DISCONNECTED -> "Disconnected" to Color(0xFFE53935)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            "Diagnostics",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        // -------------------------------------------------
        // Connection status
        // -------------------------------------------------

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {

            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = statusLabel,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(32.dp))

        HorizontalDivider()

        Spacer(Modifier.height(32.dp))

        // -------------------------------------------------
        // Pitch / Roll - large readout
        // -------------------------------------------------

        Text(
            text = "Pitch",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "${"%.1f".format(telemetry.pitch)}°",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Roll",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "${"%.1f".format(telemetry.roll)}°",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )
    }
}