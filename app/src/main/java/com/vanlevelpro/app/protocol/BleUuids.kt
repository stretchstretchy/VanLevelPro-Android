package com.vanlevelpro.app.protocol

import java.util.UUID

object BleUuids {

    val SERVICE_UUID =
        UUID.fromString("5b42c100-0001-4b5f-9999-112233445566")

    val TELEMETRY_UUID =
        UUID.fromString("5b42c100-0002-4b5f-9999-112233445566")

    val COMMAND_UUID =
        UUID.fromString("5b42c100-0003-4b5f-9999-112233445566")
}