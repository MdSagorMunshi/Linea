package com.ryanshelby.linea.telecom

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoleHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telecomManager: TelecomManager
) {

    fun isDefaultDialer(): Boolean {
        val roleManager = context.getSystemService(RoleManager::class.java)
        val roleHeld = roleManager?.let {
            if (it.isRoleAvailable(RoleManager.ROLE_DIALER)) it.isRoleHeld(RoleManager.ROLE_DIALER) else false
        } ?: false
        val telecomDefault = context.packageName == telecomManager.defaultDialerPackage
        return roleHeld || telecomDefault
    }

    fun createRequestRoleIntent(): Intent? {
        val roleManager = context.getSystemService(RoleManager::class.java)
        return if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
            roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
            }
        }
    }
}
