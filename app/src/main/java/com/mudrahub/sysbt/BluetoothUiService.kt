package com.mudrahub.sysbt

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Drives the Settings -> Bluetooth page to press "Connect" on a bonded device entry.
 * You must enable this service in: Settings > Accessibility > MudraHub > On
 */
class BluetoothUiService : AccessibilityService() {

    private var pendingAction: Action? = null

    private val rx = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val name = intent.getStringExtra("deviceName") ?: return
            val mac = intent.getStringExtra("mac") // optional; most UIs only show name
            when (intent.action) {
                "com.mudrahub.sysbt.CONNECT" -> pendingAction = Action.Connect(name, mac)
                "com.mudrahub.sysbt.DISCONNECT" -> pendingAction = Action.Disconnect(name, mac)
            }
            // Try acting immediately
            performOnCurrentWindow()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        registerReceiver(rx, IntentFilter().apply {
            addAction("com.mudrahub.sysbt.CONNECT")
            addAction("com.mudrahub.sysbt.DISCONNECT")
        })
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            performOnCurrentWindow()
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        unregisterReceiver(rx)
        super.onDestroy()
    }

    private fun performOnCurrentWindow() {
        val act = pendingAction ?: return
        val root = rootInActiveWindow ?: return

        // Find the row with the device name
        val nodes = root.findAccessibilityNodeInfosByText(act.deviceName) ?: return
        for (n in nodes) {
            // Traverse up to the device item container, then find a "Connect" or toggle button within it
            var container: AccessibilityNodeInfo? = n
            repeat(4) { container = container?.parent ?: return@repeat } // climb a few levels; OEMs vary
            val cont = container ?: continue

            val connectNode = findNodeWithAnyText(cont, listOf("Connect","Pair","Use for audio","On","Connected"))
            val disconnectNode = findNodeWithAnyText(cont, listOf("Disconnect","Off","Not connected"))

            when (act) {
                is Action.Connect -> {
                    if (clickIfAvailable(connectNode) || clickIfAvailable(cont)) {
                        pendingAction = null; return
                    }
                }
                is Action.Disconnect -> {
                    if (clickIfAvailable(disconnectNode)) {
                        pendingAction = null; return
                    }
                }
            }
        }
    }

    private fun findNodeWithAnyText(root: AccessibilityNodeInfo, texts: List<String>): AccessibilityNodeInfo? {
        for (t in texts) {
            val found = root.findAccessibilityNodeInfosByText(t)
            if (!found.isNullOrEmpty()) return found.first()
        }
        return null
    }

    private fun clickIfAvailable(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isClickable) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        // Try to click parent
        var p = node.parent
        var depth = 0
        while (p != null && depth < 4) {
            if (p.isClickable) return p.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            depth++; p = p.parent
        }
        return false
    }

    private sealed class Action(val deviceName: String, val mac: String?) {
        class Connect(name: String, mac: String?) : Action(name, mac)
        class Disconnect(name: String, mac: String?) : Action(name, mac)
    }
}
