package com.vanlevelpro.app.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomBar(
    navController: NavController
) {

    val screens = listOf(
        Screen.Dashboard,
        Screen.Calibration,
        Screen.Diagnostics,
        Screen.Settings
    )

    val currentRoute =
        navController.currentBackStackEntryAsState().value
            ?.destination
            ?.route

    NavigationBar {

        screens.forEach { screen ->

            NavigationBarItem(

                selected = currentRoute == screen.route,

                onClick = {

                    navController.navigate(screen.route) {

                        launchSingleTop = true
                        restoreState = true

                    }
                },

                icon = {

                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title
                    )
                },

                label = {

                    Text(screen.title)
                }
            )
        }
    }
}