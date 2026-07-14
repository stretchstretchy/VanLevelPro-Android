package com.vanlevelpro.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vanlevelpro.app.bluetooth.BluetoothManager
import com.vanlevelpro.app.ui.screens.MainScreen
import com.vanlevelpro.app.ui.theme.VanLevelProTheme

class MainActivity : ComponentActivity() {

    private lateinit var bluetoothManager: BluetoothManager

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        bluetoothManager = BluetoothManager(this)

        requestPermissions()

        setContent {

            val status by bluetoothManager.status.collectAsStateWithLifecycle()
            val telemetry by bluetoothManager.telemetry.collectAsStateWithLifecycle()

            VanLevelProTheme {

                MainScreen(
                    status = status,
                    pitch = telemetry.pitch,
                    roll = telemetry.roll,
                    onScan = {
                        bluetoothManager.scan()
                    }
                )
            }
        }
    }

    private fun requestPermissions() {

        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
}