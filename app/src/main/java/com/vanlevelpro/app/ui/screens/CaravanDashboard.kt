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
fun CaravanDashboard(
    status: String,
    pitch: Float,
    roll: Float,
    onScan: () -> Unit,
    greenThreshold: Float = 0.5f,
    yellowThreshold: Float = 2.0f
) {

    val connected =
        status.contains("Connected", true) ||
                status.contains("Notification", true)

    val level =
        abs(pitch) < greenThreshold &&
                abs(roll) < greenThreshold

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            "FRONT",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        CaravanGauge(
            angle = roll,
            imageRes = R.drawable.caravan_front,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            tolerance = greenThreshold,
            warningThreshold = yellowThreshold
        )

        Spacer(Modifier.height(12.dp))

        HorizontalDivider()

        Spacer(Modifier.height(12.dp))

        Text(
            "SIDE",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        CaravanGauge(
            angle = pitch,
            imageRes = R.drawable.caravan_side,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            tolerance = greenThreshold,
            warningThreshold = yellowThreshold,
            invertImageRotation = true
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (level) "LEVEL" else "LEVELLING",
            color =
                if (level)
                    Color(0xFF4CAF50)
                else
                    Color(0xFFFFB300),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
