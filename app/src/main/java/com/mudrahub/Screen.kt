package com.mudrahub

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mudrahub.data.ConnectorKind
import com.mudrahub.data.TargetDevice

@Composable
fun MudraScreen(vm: AppViewModel = viewModel()) {
    val devices by vm.devicesFlow.collectAsState(initial = emptyList())
    val status by vm.status.collectAsState(initial = "Ready")

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("MudraHub") }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            Text(status, style = MaterialTheme.typography.bodyMedium)

            // Add device row
            AddDeviceRow { name, kind, mac, uuid -> vm.addDevice(name, kind, mac, uuid) }

            Divider()

            // Device list
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices, key = { it.id }) { d ->
                    DeviceRow(d,
                        onConnect = { vm.connectTo(d) },
                        onDisconnect = { vm.disconnectFrom(d) },
                        onEdit = { vm.editDevice(it) },
                        onDelete = { vm.removeDevice(d.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddDeviceRow(onAdd: (String, ConnectorKind, String, String?) -> Unit) {
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var mac by remember { mutableStateOf(TextFieldValue("")) }
    var uuid by remember { mutableStateOf(TextFieldValue("")) }
    var kind by remember { mutableStateOf(ConnectorKind.SYSTEM_BT) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Add Device")
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Label (e.g., Room TV)") }, modifier = Modifier.fillMaxWidth())
        KindDropdown(kind) { kind = it }
        OutlinedTextField(value = mac, onValueChange = { mac = it }, label = { Text("MAC (optional for SystemBt; required for SPP/BLE)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = uuid, onValueChange = { uuid = it }, label = { Text("SPP UUID (optional)") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                onAdd(name.text.trim(), kind, mac.text.trim(), uuid.text.takeIf { it.isNotBlank() })
                name = TextFieldValue(""); mac = TextFieldValue(""); uuid = TextFieldValue("")
            }) { Text("Add") }
        }
    }
}

@Composable
private fun KindDropdown(current: ConnectorKind, onPick: (ConnectorKind) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(value = current.name, onValueChange = {}, readOnly = true, modifier = Modifier.menuAnchor(), label = { Text("Connector") })
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ConnectorKind.values().forEach { k -> DropdownMenuItem(text = { Text(k.name) }, onClick = { onPick(k); expanded = false }) }
        }
    }
}

@Composable
private fun DeviceRow(
    d: TargetDevice,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onEdit: (TargetDevice) -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(d.name, style = MaterialTheme.typography.titleMedium)
            Text("${d.connector} • ${if (d.address.isBlank()) "No MAC set" else d.address}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConnect) { Text("Connect") }
                OutlinedButton(onClick = onDisconnect) { Text("Disconnect") }
                OutlinedButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}
