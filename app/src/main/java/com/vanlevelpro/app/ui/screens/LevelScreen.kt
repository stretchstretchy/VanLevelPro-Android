package com.vanlevelpro.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.vanlevelpro.app.viewmodel.MainViewModel

@Composable
fun LevelScreen(
    viewModel: MainViewModel
) {

    val telemetry by viewModel.telemetry.collectAsState()

    val status by viewModel.status.collectAsState()

    val greenThreshold by viewModel.greenThreshold.collectAsState()

    val yellowThreshold by viewModel.yellowThreshold.collectAsState()

    CaravanDashboard(

        status = status,

        pitch = telemetry.pitch,

        roll = telemetry.roll,

        onScan = {
            viewModel.scan()
        },

        greenThreshold = greenThreshold,

        yellowThreshold = yellowThreshold

    )

}