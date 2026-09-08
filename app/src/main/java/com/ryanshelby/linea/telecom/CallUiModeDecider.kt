package com.ryanshelby.linea.telecom

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class CallUiMode {
    FULL_SCREEN,
    MINI_FLOAT
}

@Singleton
class CallUiModeDecider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun decideUiMode(): CallUiMode {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isInteractive = powerManager?.isInteractive ?: true
        if (!isInteractive) {
            // Screen is OFF -> Full Screen
            return CallUiMode.FULL_SCREEN
        }

        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isLocked = keyguardManager?.isKeyguardLocked ?: false
        if (isLocked) {
            // Lock screen is active -> Full Screen
            return CallUiMode.FULL_SCREEN
        }

        // Screen is ON and phone is UNLOCKED.
        // Check if user is on the home screen / launcher (not inside an app)
        if (isUserOnHomeScreen()) {
            return CallUiMode.FULL_SCREEN
        }

        // User is inside another application -> Mini Call Float
        return CallUiMode.MINI_FLOAT
    }

    private fun isUserOnHomeScreen(): Boolean {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = context.packageManager.resolveActivity(
                homeIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            val launcherPackage = resolveInfo?.activityInfo?.packageName

            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
            @Suppress("DEPRECATION")
            val tasks = am.getRunningTasks(1)
            if (!tasks.isNullOrEmpty()) {
                val topPkg = tasks[0].topActivity?.packageName
                if (topPkg != null) {
                    if (launcherPackage != null && topPkg == launcherPackage) {
                        return true
                    }
                    if (topPkg.contains("launcher", ignoreCase = true) ||
                        topPkg.contains("quickstep", ignoreCase = true) ||
                        topPkg.contains("nexuslauncher", ignoreCase = true) ||
                        topPkg.contains("trebuchet", ignoreCase = true) ||
                        topPkg.contains("home", ignoreCase = true)) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            // In case of permission or sandbox restrictions
        }
        return false
    }
}
