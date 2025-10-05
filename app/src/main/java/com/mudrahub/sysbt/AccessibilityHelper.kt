package com.mudrahub.sysbt

import android.content.Context
import android.content.Intent
import android.provider.Settings

object AccessibilityHelper {

    /**
     * Returns true if Accessibility is enabled globally on the device.
     * (Good enough for showing/hiding the in-app banner.)
     */
    fun isServiceEnabled(context: Context): Boolean {
        // 1 = enabled, 0 = disabled
        val enabled = try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (_: Throwable) {
            0
        }
        return enabled == 1
    }

    /** Opens the Accessibility Settings screen so the user can enable services. */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Convenience alias used elsewhere in the app. */
    fun hasAccessibilityPermission(context: Context): Boolean = isServiceEnabled(context)
}
