package com.ryanshelby.linea.telecom

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
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
    private val telephonyManager: TelephonyManager
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
        val registeredAccounts = mutableListOf<SimAccountInfo>()
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)

        val subscriptions: List<SubscriptionInfo>? = try {
            subscriptionManager?.activeSubscriptionInfoList
        } catch (_: SecurityException) {
            null
        }

        try {
            val callCapableHandles = telecomManager.callCapablePhoneAccounts.orEmpty()

            // Do not infer a subscription from the position of a phone account. The list can
            // contain non-SIM accounts and its order is OEM-dependent. TelephonyManager is the
            // public API that maps a PhoneAccountHandle to its actual subscription ID.
            val handlesBySubscriptionId = callCapableHandles.mapNotNull { handle ->
                val subscriptionId = try {
                    telephonyManager.getSubscriptionId(handle)
                } catch (_: SecurityException) {
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID
                }
                if (SubscriptionManager.isValidSubscriptionId(subscriptionId)) {
                    subscriptionId to handle
                } else {
                    null
                }
            }.toMap()

            subscriptions.orEmpty()
                .sortedBy { it.simSlotIndex }
                .forEach { subInfo ->
                    // An explicit SIM must have its own enabled phone-account handle. Never
                    // reuse the system-default handle for another subscription.
                    val handle = handlesBySubscriptionId[subInfo.subscriptionId] ?: return@forEach
                    val phoneAccount = telecomManager.getPhoneAccount(handle)
                    val displayName = subInfo.displayName?.toString()
                        ?: phoneAccount?.label?.toString()
                        ?: "SIM ${subInfo.simSlotIndex + 1}"
                    val carrierName = subInfo.carrierName?.toString()
                        ?: phoneAccount?.shortDescription?.toString()
                        ?: displayName
                    registeredAccounts.add(
                        SimAccountInfo(
                            slotIndex = subInfo.simSlotIndex,
                            subscriptionId = subInfo.subscriptionId,
                            displayName = displayName,
                            carrierName = carrierName,
                            phoneAccountHandle = handle
                        )
                    )
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return registeredAccounts
    }
}
