package com.mudrahub.sysbt

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager

/**
 * Utilities for checking whether our AccessibilityService is enabled,
 * and for opening the Accessibility settings screen.
 */
object AccessibilityHelper {

    fun isServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        // Quick path: walk through enabled services list
        val enabled = am.enabledAccessibilityServiceList
        val myServiceId = ComponentIds.serviceId(context) // e.g. "com.mudrahub/com.mudrahub.sysbt.BluetoothUiService"
        if (enabled.any { it.id == myServiceId }) return true

        // Fallback: read secure setting in case list is empty on some OEMs
        return try {
            val setting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            !setting.isNullOrEmpty() && TextUtils.SimpleStringSplitter(':').apply { setString(setting) }
                .any { it.equals(myServiceId, ignoreCase = true) }
        } catch (_: Throwable) {
            false
        }
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private object ComponentIds {
        fun serviceId(context: Context): String {
            val pkg = context.packageName
            val cls = "com.mudrahub.sysbt.BluetoothUiService"
            return "$pkg/$cls"
        }
    }
}
