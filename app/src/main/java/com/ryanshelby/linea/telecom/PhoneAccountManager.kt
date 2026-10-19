package com.ryanshelby.linea.telecom

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class SimAccountInfo(
    val slotIndex: Int,
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val phoneAccountHandle: PhoneAccountHandle
)

@Singleton
class PhoneAccountManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telecomManager: TelecomManager,
    private val telephonyManager: TelephonyManager,
    private val simAccountResolver: SimAccountResolver
) {

    init {
        unregisterLegacyAccounts()
    }

    private fun unregisterLegacyAccounts() {
        try {
            val legacyComponent = ComponentName(context, "com.ryanshelby.linea.telecom.LineaConnectionService")
            telecomManager.unregisterPhoneAccount(PhoneAccountHandle(legacyComponent, "linea_default_sim"))
            for (i in 0..10) {
                telecomManager.unregisterPhoneAccount(PhoneAccountHandle(legacyComponent, "linea_sim_$i"))
            }
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    fun registerPhoneAccounts(): List<SimAccountInfo> {
        return getSimAccounts()
    }

    @SuppressLint("MissingPermission")
    fun getSimAccounts(): List<SimAccountInfo> {
        return simAccountResolver.refresh()
    }

    @SuppressLint("MissingPermission")
    fun getDefaultOutgoingPhoneAccount(uriScheme: String = "tel"): PhoneAccountHandle? {
        return try {
            telecomManager.getDefaultOutgoingPhoneAccount(uriScheme)
        } catch (_: Exception) {
            null
        }
    }
}

