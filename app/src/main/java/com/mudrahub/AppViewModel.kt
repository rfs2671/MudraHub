package com.mudrahub

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mudrahub.connect.BleConnector
import com.mudrahub.connect.Connector
import com.mudrahub.connect.SppConnector
import com.mudrahub.connect.SystemBtConnector
import com.mudrahub.data.ConnectorKind
import com.mudrahub.data.Repo
import com.mudrahub.data.TargetDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context get() = getApplication<Application>().applicationContext
    private val repo = Repo()

    val devicesFlow = repo.devicesFlow

    private val system = SystemBtConnector()
    private val spp = SppConnector()
    private val ble = BleConnector()

    private val _status = MutableStateFlow("Ready")
    val status = _status.asStateFlow()

    private var lastConnected: TargetDevice? = null

    fun addDevice(name: String, kind: ConnectorKind, address: String, serviceUuid: String?) {
        repo.upsert(TargetDevice(UUID.randomUUID().toString(), name, kind, address, serviceUuid))
    }
    fun editDevice(d: TargetDevice) = repo.upsert(d)
    fun removeDevice(id: String) = repo.remove(id)

    fun connectTo(d: TargetDevice) = viewModelScope.launch {
        _status.value = "Connecting to ${d.name}…"
        // We don't “disconnect others” via system BT (not allowed by API); for SPP/BLE we close any held sessions.
        if (lastConnected != null && lastConnected!!.connector != ConnectorKind.SYSTEM_BT) {
            disconnectFrom(lastConnected!!)
        }

        val ok = when (d.connector) {
            ConnectorKind.SYSTEM_BT -> system.connect(ctx, d)
            ConnectorKind.SPP -> spp.connect(ctx, d)
            ConnectorKind.BLE -> ble.connect(ctx, d)
        }
        _status.value = if (ok) "Connected (requested) → ${d.name}" else "Failed to connect ${d.name}"
        lastConnected = d
    }

    fun disconnectFrom(d: TargetDevice) = viewModelScope.launch {
        _status.value = "Disconnecting ${d.name}…"
        val ok = when (d.connector) {
            ConnectorKind.SYSTEM_BT -> system.disconnect(ctx, d) // tries via UI if visible
            ConnectorKind.SPP -> spp.disconnect(ctx, d)
            ConnectorKind.BLE -> ble.disconnect(ctx, d)
        }
        _status.value = if (ok) "Disconnected ${d.name}" else "Failed to disconnect ${d.name}"
        if (lastConnected?.id == d.id) lastConnected = null
    }
}
