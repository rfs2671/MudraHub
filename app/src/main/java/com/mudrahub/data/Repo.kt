package com.mudrahub.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class Repo {
    private val _devices = MutableStateFlow<List<TargetDevice>>(listOf(
        TargetDevice(id = UUID.randomUUID().toString(), name = "Room TV", connector = ConnectorKind.SYSTEM_BT),
        TargetDevice(id = UUID.randomUUID().toString(), name = "Car", connector = ConnectorKind.SYSTEM_BT),
        TargetDevice(id = UUID.randomUUID().toString(), name = "Workshop SPP", connector = ConnectorKind.SPP, address = "00:11:22:33:44:55"),
        TargetDevice(id = UUID.randomUUID().toString(), name = "AR Glasses (BLE)", connector = ConnectorKind.BLE, address = "AA:BB:CC:DD:EE:FF")
    ))
    val devicesFlow = _devices.asStateFlow()

    fun upsert(d: TargetDevice) {
        val list = _devices.value.toMutableList()
        val idx = list.indexOfFirst { it.id == d.id }
        if (idx >= 0) list[idx] = d else list += d
        _devices.value = list
    }
    fun remove(id: String) {
        _devices.value = _devices.value.filterNot { it.id == id }
    }
}
