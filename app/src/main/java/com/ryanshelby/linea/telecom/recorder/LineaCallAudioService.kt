package com.ryanshelby.linea.telecom.recorder

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager

/**
 * Passive Accessibility Service for Linea.
 *
 * Exemption Mechanism:
 * On Android 10+ (Android 10, 11, 12, 13, 14, 15), AOSP AudioPolicyManager marks any standard
 * third-party app as `isSilenced = true` during active phone calls (MODE_IN_CALL), streaming zeros.
 * AudioPolicyService explicitly exempts Accessibility Clients (`isAccessibilityClient(uid)`),
 * allowing un-silenced microphone capture for both live ECG visualizers and acoustic call recording.
 */
class LineaCallAudioService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "LineaCallAudioService connected – un-silenced call audio capture enabled")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Passive service – no UI event processing required
    }

    override fun onInterrupt() {
        Log.w(TAG, "LineaCallAudioService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "LineaCallAudioService destroyed")
    }

    companion object {
        private const val TAG = "LineaCallAudioService"

        /**
         * Checks whether the user has toggled ON LineaCallAudioService in system Accessibility Settings.
         */
        fun isServiceEnabled(context: Context): Boolean {
            return try {
                // Primary check: Query system AccessibilityManager for active enabled services
                val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                val enabledList = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                if (enabledList != null) {
                    val isFound = enabledList.any { service ->
                        val sInfo = service.resolveInfo?.serviceInfo
                        sInfo != null &&
                            sInfo.packageName == context.packageName &&
                            (sInfo.name == LineaCallAudioService::class.java.name ||
                             sInfo.name.endsWith("LineaCallAudioService"))
                    }
                    if (isFound) return true
                }

                // Secondary fallback: Parse Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                val expectedComponent = ComponentName(context, LineaCallAudioService::class.java)
                val fullString = expectedComponent.flattenToString()
                val shortString = expectedComponent.flattenToShortString()
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: return false

                enabledServices.split(":").any {
                    it.equals(fullString, ignoreCase = true) ||
                    it.equals(shortString, ignoreCase = true) ||
                    it.contains("LineaCallAudioService", ignoreCase = true)
                }
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Opens the system Accessibility Settings screen where user can enable LineaCallAudioService.
         */
        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open accessibility settings: ${e.message}")
            }
        }
    }
}
