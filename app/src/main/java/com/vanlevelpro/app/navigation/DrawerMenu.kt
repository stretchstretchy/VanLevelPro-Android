package com.vanlevelpro.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vanlevelpro.app.ui.screens.CalibrationScreen
import com.vanlevelpro.app.ui.screens.DiagnosticsScreen
import com.vanlevelpro.app.ui.screens.LevelScreen
import com.vanlevelpro.app.ui.screens.SettingsScreen
import com.vanlevelpro.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerMenu(
    viewModel: MainViewModel
) {

    val navController = rememberNavController()

    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )

    val scope = rememberCoroutineScope()

    var selected by remember {
        mutableStateOf(Screen.Level.route)
    }

    ModalNavigationDrawer(

        drawerState = drawerState,

        drawerContent = {

            ModalDrawerSheet {

                Text(
                    text = "VanLevel Pro",
                    modifier = Modifier.padding(20.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Level") },
                    selected = selected == Screen.Level.route,
                    onClick = {
                        selected = Screen.Level.route
                        navController.navigate(Screen.Level.route)
                        scope.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Calibration") },
                    selected = selected == Screen.Calibration.route,
                    onClick = {
                        selected = Screen.Calibration.route
                        navController.navigate(Screen.Calibration.route)
                        scope.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Diagnostics") },
                    selected = selected == Screen.Diagnostics.route,
                    onClick = {
                        selected = Screen.Diagnostics.route
                        navController.navigate(Screen.Diagnostics.route)
                        scope.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = selected == Screen.Settings.route,
                    onClick = {
                        selected = Screen.Settings.route
                        navController.navigate(Screen.Settings.route)
                        scope.launch { drawerState.close() }
                    }
                )

                Text(
                    text = "Version 0.5",
                    modifier = Modifier.padding(20.dp)
                )
            }
        }

    ) {

        Scaffold(

            topBar = {

                TopAppBar(

                    title = {
                        Text("VanLevel Pro")
                    },

                    navigationIcon = {

                        IconButton(

                            onClick = {

                                scope.launch {
                                    drawerState.open()
                                }

                            }

                        ) {

                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu"
                            )

                        }

                    },

                    actions = {

                        Text(
                            text = "●",
                            color = Color.Green,
                            modifier = Modifier.padding(end = 16.dp)
                        )

                    }

                )

            }

        ) { padding ->

            NavHost(

                navController = navController,

                startDestination = Screen.Level.route,

                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)

            ) {

                composable(Screen.Level.route) {
                    LevelScreen(viewModel)
                }

                composable(Screen.Calibration.route) {
                    CalibrationScreen(viewModel)
                }

                composable(Screen.Diagnostics.route) {
                    DiagnosticsScreen(viewModel)
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(viewModel)
                }

            }

        }

    }

}