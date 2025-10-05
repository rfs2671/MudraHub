package com.mudrahub.sysbt

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

object AccessibilityHelper {

    /** Returns true if the specified accessibility service is enabled. */
    fun isServiceEnabled(
        context: Context,
        service: Class<out AccessibilityService>
    ): Boolean {
        val setting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        // Split the colon-delimited list and compare against the flattened component name
        val target = ComponentName(context, service).flattenToString()
        return setting.split(':').any { entry ->
            entry.equals(target, ignoreCase = true)
        }
    }

    /** Opens the Accessibility Settings screen so the user can enable the service. */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Convenience: some code prefers this name. */
    fun hasAccessibilityPermission(
        context: Context,
        service: Class<out AccessibilityService>
    ): Boolean = isServiceEnabled(context, service)
}
