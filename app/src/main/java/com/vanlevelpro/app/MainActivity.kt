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

        val permissions = mutableListOf<String>()

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissions.isEmpty()) {

            Log.e("TEST", "Permissions Already Granted - Starting Scan")
            viewModel.scan()

        } else {

            permissionLauncher.launch(
                permissions.toTypedArray()
            )

        }
    }
}