package com.mudrahub.data

enum class ConnectorKind { SYSTEM_BT, SPP, BLE }

data class TargetDevice(
    val id: String,
    val name: String,
    val connector: ConnectorKind,
    val address: String = "",     // MAC for SYSTEM_BT & SPP; BLE address too
    val serviceUuid: String? = null // for SPP custom UUID or BLE service
)
