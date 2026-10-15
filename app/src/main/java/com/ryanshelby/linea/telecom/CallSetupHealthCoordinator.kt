package com.ryanshelby.linea.telecom

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.ryanshelby.linea.LineaApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class CallHandlingHealth(
    val isDefaultDialer: Boolean,
    val isFullScreenIntentAllowed: Boolean,
    val areNotificationsEnabled: Boolean,
    val isBatteryOptimizationIgnored: Boolean
) {
    val allPassed: Boolean
        get() = isDefaultDialer && isFullScreenIntentAllowed && areNotificationsEnabled

    val failureCount: Int
        get() = (if (!isDefaultDialer) 1 else 0) +
                (if (!isFullScreenIntentAllowed) 1 else 0) +
                (if (!areNotificationsEnabled) 1 else 0)
}

@Singleton
class CallSetupHealthCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val roleHelper: RoleHelper
) {

    fun checkHealth(): CallHandlingHealth {
        val isDefault = roleHelper.isDefaultDialer()

        val isFullScreenAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = context.getSystemService(NotificationManager::class.java)
            try {
                nm?.canUseFullScreenIntent() ?: true
            } catch (_: Exception) { true }
        } else {
            true
        }

        val areNotifsEnabled = run {
            val compatEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            if (!compatEnabled) return@run false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(NotificationManager::class.java)
                val incomingChannel = nm?.getNotificationChannel(LineaApp.CHANNEL_INCOMING_CALLS)
                if (incomingChannel != null && incomingChannel.importance == NotificationManager.IMPORTANCE_NONE) {
                    return@run false
                }
            }
            true
        }

        // When an app is set as the Default Phone Dialer (ROLE_DIALER), the Android OS
        // automatically exempts it from Doze, App Standby, and background limits (PowerExemptionManager.REASON_ROLE_DIALER).
        // Therefore, if the app is the default dialer, background execution is already unrestricted.
        val isBatteryIgnored = if (isDefault) {
            true
        } else {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            try {
                pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
            } catch (_: Exception) { true }
        }

        return CallHandlingHealth(
            isDefaultDialer = isDefault,
            isFullScreenIntentAllowed = isFullScreenAllowed,
            areNotificationsEnabled = areNotifsEnabled,
            isBatteryOptimizationIgnored = isBatteryIgnored
        )
    }

    fun createRequestDefaultDialerIntent(): Intent? = roleHelper.createRequestRoleIntent()

    fun createFullScreenIntentSettings(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    fun createNotificationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    }

    fun createBatteryOptimizationIntent(): Intent {
        return try {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } catch (_: Exception) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        }
    }
}
