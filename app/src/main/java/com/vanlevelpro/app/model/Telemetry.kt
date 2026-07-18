package com.vanlevelpro.app.model

data class Telemetry(
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val connected: Boolean = false,
    val calibrated: Boolean = false
)