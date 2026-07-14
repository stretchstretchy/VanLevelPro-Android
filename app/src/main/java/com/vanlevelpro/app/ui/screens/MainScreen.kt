package com.vanlevelpro.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vanlevelpro.app.R
import com.vanlevelpro.app.ui.components.gauges.CaravanGauge
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    status: String,
    pitch: Float,
    roll: Float,
    onScan: () -> Unit
) {

    val connected =
        status.contains("Connected", true) ||
                status.contains("Notification", true)

    val level =
        abs(pitch) < 0.5f &&
                abs(roll) < 0.5f

    Scaffold(

        topBar = {

            CenterAlignedTopAppBar(

                title = {

                    Text(
                        "VanLevel Pro",
                        fontWeight = FontWeight.Bold
                    )
                },

                actions = {

                    Text(
                        if (connected) "🟢" else "⚪",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(Modifier.width(12.dp))
                }
            )
        }

    ) { padding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),

            horizontalAlignment = Alignment.CenterHorizontally

        ) {

            Text(
                "FRONT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Pitch: ${"%.1f".format(pitch)}°",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Roll: ${"%.1f".format(roll)}°",
                style = MaterialTheme.typography.bodyLarge
            )

            CaravanGauge(
                angle = roll,
                imageRes = R.drawable.caravan_front,
                modifier = Modifier.size(300.dp)
            )

            Spacer(Modifier.height(24.dp))

            HorizontalDivider()

            Spacer(Modifier.height(24.dp))

            Text(
                "SIDE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            CaravanGauge(
                angle = pitch,
                imageRes = R.drawable.caravan_side,
                modifier = Modifier.size(370.dp)
            )


            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = if (level) "LEVEL" else "LEVELLING",
                color =
                    if (level)
                        Color(0xFF4CAF50)
                    else
                        Color(0xFFFFB300),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}