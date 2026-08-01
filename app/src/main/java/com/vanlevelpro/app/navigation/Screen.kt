package com.vanlevelpro.app.navigation

sealed class Screen(
    val route: String,
    val title: String
) {

    object Level : Screen(
        route = "level",
        title = "Level"
    )

    object Calibration : Screen(
        route = "calibration",
        title = "Calibration"
    )

    object Diagnostics : Screen(
        route = "diagnostics",
        title = "Diagnostics"
    )

    object Settings : Screen(
        route = "settings",
        title = "Settings"
    )

    object Firmware : Screen(
        route = "firmware",
        title = "Firmware Update"
    )
}