package com.mudrahub.connect

import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.mudrahub.data.TargetDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanResult
import java.io.OutputStream
import java.util.UUID

/** Base contract each connector implements */
interface Connector {
    suspend fun connect(ctx: Context, target: TargetDevice): Boolean
    suspend fun disconnect(ctx: Context, target: TargetDevice): Boolean
}

/** 1) SYSTEM_BT: drives System Bluetooth UI via our AccessibilityService helper */
class SystemBtConnector : Connector {
    override suspend fun connect(ctx: Context, target: TargetDevice): Boolean = withContext(Dispatchers.Main) {
        // Launch Bluetooth Settings filtered to the device list; our AccessibilityService will click the target name.
        ctx.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        // Signal the service which device to press
        ctx.sendBroadcast(Intent("com.mudrahub.sysbt.CONNECT").putExtra("deviceName", target.name).putExtra("mac", target.address))
        true
    }
    override suspend fun disconnect(ctx: Context, target: TargetDevice): Boolean {
        // We can open settings and let the service press "Disconnect" if visible. Many UIs reconnect automatically; optional.
        ctx.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        ctx.sendBroadcast(Intent("com.mudrahub.sysbt.DISCONNECT").putExtra("deviceName", target.name).putExtra("mac", target.address))
        return true
    }
}

/** 2) SPP: RFCOMM socket connect; this opens a session and closes it for "connect test" or keeps open if needed */
class SppConnector(private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")) : Connector {
    override suspend fun connect(ctx: Context, target: TargetDevice): Boolean = withContext(Dispatchers.IO) {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return@withContext false
        val dev = runCatching { adapter.getRemoteDevice(target.address) }.getOrNull() ?: return@withContext false
        runCatching {
            adapter.cancelDiscovery()
            val sock = dev.createRfcommSocketToServiceRecord(sppUuid)
            sock.connect()  // blocks until connected or fails
            // Optional: hold connection; here we close immediately to just trigger link & trust
            sock.outputStream.write("PING\n".toByteArray())
            sock.outputStream.flush()
            sock.close()
            true
        }.getOrDefault(false)
    }
    override suspend fun disconnect(ctx: Context, target: TargetDevice): Boolean = true // closing the socket ends session
}

/** 3) BLE: connects to a specific device and immediately disconnects (or keep if you want) */
class BleConnector : Connector {
    private var gatt: BluetoothGatt? = null
    override suspend fun connect(ctx: Context, target: TargetDevice): Boolean = withContext(Dispatchers.IO) {
        val adapter = (ctx.getSystemService(BluetoothManager::class.java))?.adapter ?: return@withContext false
        val dev = runCatching { adapter.getRemoteDevice(target.address) }.getOrNull() ?: return@withContext false
        var ok = false
        val latch = java.util.concurrent.CountDownLatch(1)
        val cb = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) { ok = true; latch.countDown() }
                if (newState == BluetoothProfile.STATE_DISCONNECTED) { latch.countDown() }
            }
        }
        gatt = dev.connectGatt(ctx, false, cb, BluetoothDevice.TRANSPORT_LE)
        latch.await()
        true == ok
    }
    override suspend fun disconnect(ctx: Context, target: TargetDevice): Boolean = withContext(Dispatchers.IO) {
        gatt?.disconnect(); gatt?.close(); gatt = null; true
    }
}
