package com.vanlevelpro.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(
    status: String,
    pitch: Float,
    roll: Float,
    onScan: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {

            Text(
                text = "VanLevel Pro Dev",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card {

                Column(modifier = Modifier.padding(16.dp)) {

                    Text("Status")

                    Text(status)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card {

                Column(modifier = Modifier.padding(16.dp)) {

                    Text("Pitch : %.1f°".format(pitch))

                    Text("Roll  : %.1f°".format(roll))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onScan
            ) {
                Text("Scan")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card {

                Column(modifier = Modifier.padding(16.dp)) {

                    Text("Log")

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Waiting...")
                }
            }
        }
    }
}