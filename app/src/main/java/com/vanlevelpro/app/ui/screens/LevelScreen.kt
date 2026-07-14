package com.vanlevelpro.app.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun LevelScreen() {

    CaravanDashboard(
        status = "Disconnected",
        pitch = 0f,
        roll = 0f,
        onScan = { }
    )

}