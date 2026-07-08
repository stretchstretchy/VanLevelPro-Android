package com.vanlevelpro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vanlevelpro.app.ui.screens.MainScreen
import com.vanlevelpro.app.ui.theme.VanLevelProTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            var status by mutableStateOf("Disconnected")

            VanLevelProTheme {

                MainScreen(
                    status = status,
                    pitch = 0.0f,
                    roll = 0.0f,
                    onScan = {
                        status = "Scanning..."
                    }
                )
            }
        }
    }
}