package com.vanlevelpro.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vanlevelpro.app.ui.screens.CalibrationScreen
import com.vanlevelpro.app.ui.screens.DashboardScreen
import com.vanlevelpro.app.ui.screens.DiagnosticsScreen
import com.vanlevelpro.app.ui.screens.SettingsScreen

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomBar(navController)
        }
    ) { padding ->

        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {

            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }

            composable(Screen.Calibration.route) {
                CalibrationScreen()
            }

            composable(Screen.Diagnostics.route) {
                DiagnosticsScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}