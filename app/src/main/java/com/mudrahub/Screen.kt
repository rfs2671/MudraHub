@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mudrahub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mudrahub.data.ConnectorKind
import com.mudrahub.data.TargetDevice
import com.mudrahub.sysbt.AccessibilityHelper
import kotlinx.coroutines.delay

@Composable
fun MudraScreen(vm: AppViewModel = viewModel()) {
    val devices by vm.devicesFlow.collectAsState(initial = emptyList())
    val status by vm.status.collectAsState(initial = "Ready")
    val ctx = androidx.compose.ui.platform.LocalContext.current

    // Poll while visible so the banner auto-hides once user enables the service and returns
    var a11yEnabled by remember { mutableStateOf(AccessibilityHelper.isServiceEnabled(ctx)) }
    LaunchedEffect(Unit) {
        while (true) {
            a11yEnabled = AccessibilityHelper.isServiceEnabled(ctx)
            delay(600)
        }
    }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("MudraHub") }) }) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Accessibility banner (only for SystemBt flow)
            if (!a11yEnabled) {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Enable Accessibility", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Turn on MudraHub in Accessibility so the app can auto-press " +
                                "Connect on the Bluetooth settings screen for your chosen device."
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { AccessibilityHelper.openAccessibilitySettings(ctx) }) {
                                Text("Open Settings")
                            }
                            OutlinedButton(onClick = {
                                a11yEnabled = AccessibilityHelper.isServiceEnabled(ctx)
                            }) {
                                Text("Refresh")
                            }
                        }
                    }
                }
            }

            Text(status, style = MaterialTheme.typography.bodyMedium)

            // Add device row
            AddDeviceRow { name, kind, mac, uuid ->
                vm.addDevice(name, kind, mac, uuid)
            }

            Divider()

            // Device list
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices, key = { it.id }) { d ->
                    DeviceRow(
                        d,
                        onConnect = { vm.connectTo(d) },
                        onDisconnect = { vm.disconnectFrom(d) },
                        onEdit = { vm.editDevice(it) },  // keep API shape; button optional below
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
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Label (e.g., Room TV)") },
            modifier = Modifier.fillMaxWidth()
        )
        KindDropdown(kind) { kind = it }
        OutlinedTextField(
            value = mac,
            onValueChange = { mac = it },
            label = { Text("MAC (optional for SystemBt; required for SPP/BLE)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uuid,
            onValueChange = { uuid = it },
            label = { Text("SPP UUID (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                onAdd(
                    name.text.trim(),
                    kind,
                    mac.text.trim(),
                    uuid.text.takeIf { txt -> txt.isNotBlank() }
                )
                name = TextFieldValue("")
                mac = TextFieldValue("")
                uuid = TextFieldValue("")
            }) { Text("Add") }
        }
    }
}

@Composable
private fun KindDropdown(current: ConnectorKind, onPick: (ConnectorKind) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = current.name,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor(),
            label = { Text("Connector") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ConnectorKind.values().forEach { k ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(k.name) },
                    onClick = { onPick(k); expanded = false }
                )
            }
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
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(d.name, style = MaterialTheme.typography.titleMedium)
            Text("${d.connector} • ${if (d.address.isBlank()) "No MAC set" else d.address}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConnect) { Text("Connect") }
                OutlinedButton(onClick = onDisconnect) { Text("Disconnect") }
                // If you want an Edit button, uncomment the next line:
                // OutlinedButton(onClick = { onEdit(d) }) { Text("Edit") }
                OutlinedButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}
