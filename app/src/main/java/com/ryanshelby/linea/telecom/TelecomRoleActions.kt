package com.ryanshelby.linea.telecom

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat

/** The platform also authorizes these actions through the default-dialer role. */
object TelecomRoleActions {
    internal fun isAuthorized(packageName: String, defaultDialer: String?, hasPhoneStatePrivilege: Boolean): Boolean =
        packageName == defaultDialer || hasPhoneStatePrivilege

    // SDK permission annotations omit the documented default-dialer alternative.
    // Each call checks the current role/permission and handles role revocation races.
    @SuppressLint("MissingPermission")
    fun cancelMissedCalls(context: Context, telecom: TelecomManager? = context.getSystemService(TelecomManager::class.java)): Boolean =
        withAuthorization(context, telecom) { cancelMissedCallsNotification() }

    @SuppressLint("MissingPermission")
    fun silenceRinger(context: Context, telecom: TelecomManager?): Boolean =
        withAuthorization(context, telecom) { silenceRinger() }

    private inline fun withAuthorization(context: Context, telecom: TelecomManager?, action: TelecomManager.() -> Unit): Boolean {
        if (telecom == null) return false
        return try {
            val privileged = ContextCompat.checkSelfPermission(context, Manifest.permission.MODIFY_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
            if (!isAuthorized(context.packageName, telecom.defaultDialerPackage, privileged)) return false
            telecom.action()
            true
        } catch (e: SecurityException) {
            Log.w("TelecomRoleActions", "Telecom authorization changed before the action completed", e)
            false
        } catch (e: UnsupportedOperationException) {
            Log.w("TelecomRoleActions", "Telecom action is unavailable on this device", e)
            false
        }
    }
}
