package com.vanlevelpro.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {

    object Dashboard : Screen(
        "dashboard",
        "Dashboard",
        Icons.Default.Home
    )

    object Calibration : Screen(
        "calibration",
        "Calibration",
        Icons.Default.Info
    )

    object Diagnostics : Screen(
        "diagnostics",
        "Diagnostics",
        Icons.Default.Build
    )

    object Settings : Screen(
        "settings",
        "Settings",
        Icons.Default.Settings
    )
}