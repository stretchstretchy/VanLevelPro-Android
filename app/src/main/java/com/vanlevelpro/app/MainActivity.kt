package com.vanlevelpro.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.vanlevelpro.app.model.ConnectionState
import com.vanlevelpro.app.navigation.DrawerMenu
import com.vanlevelpro.app.ui.theme.VanLevelProTheme
import com.vanlevelpro.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val allGranted = permissions.values.all { it }

            if (allGranted) {
                Log.e("TEST", "Permissions Granted - Starting Scan")
                viewModel.scan()
            } else {
                Log.e("TEST", "Permissions Denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.e("TEST", "MainActivity onCreate")

        enableEdgeToEdge()

        setContent {
            VanLevelProTheme {
                DrawerMenu(viewModel)
            }
        }

        requestPermissions()
    }

    override fun onStart() {
        super.onStart()
        Log.e("TEST", "MainActivity onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.e("TEST", "MainActivity onResume")

        // onStop() deliberately disconnects (e.g. when the screen times
        // out and the activity is no longer visible). Nothing else was
        // re-triggering a scan/reconnect on the way back - previously
        // this only worked on a fresh app launch (onCreate), forcing a
        // full force-stop-and-relaunch to reconnect. Re-scan here if
        // we're not already connected/connecting/scanning, and only if
        // permissions are actually granted (avoids racing the
        // first-launch permission dialog).
        if (
            viewModel.connectionState.value == ConnectionState.DISCONNECTED &&
            hasAllPermissions()
        ) {
            Log.e("TEST", "onResume - was disconnected, re-scanning")
            viewModel.scan()
        }
    }

    private fun hasAllPermissions(): Boolean {

        val required = listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        return required.all {
            ContextCompat.checkSelfPermission(this, it) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onPause() {
        Log.e("TEST", "MainActivity onPause")
        super.onPause()
    }

    override fun onStop() {

        Log.e("TEST", "MainActivity onStop")

        // Disconnect HERE instead of onDestroy()
        viewModel.disconnect()

        super.onStop()
    }

    override fun onDestroy() {

        Log.e("TEST", "MainActivity onDestroy")

        super.onDestroy()
    }

    private fun requestPermissions() {

        if (hasAllPermissions()) {

            Log.e("TEST", "Permissions Already Granted - Starting Scan")
            viewModel.scan()

        } else {

            val permissions = mutableListOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

            permissionLauncher.launch(
                permissions.toTypedArray()
            )

        }
    }
}