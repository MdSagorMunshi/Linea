package com.ryanshelby.linea.telecom

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
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
    private val telecomManager: TelecomManager
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
            val callCapableHandles = telecomManager.callCapablePhoneAccounts
            if (!callCapableHandles.isNullOrEmpty()) {
                callCapableHandles.forEachIndexed { index, handle ->
                    val phoneAccount = telecomManager.getPhoneAccount(handle)
                    val matchingSub = subscriptions?.firstOrNull { sub ->
                        handle.id.contains(sub.subscriptionId.toString()) ||
                        handle.id.contains(sub.iccId ?: "---") ||
                        sub.simSlotIndex == index
                    }

                    val slotIndex = matchingSub?.simSlotIndex ?: index
                    val subId = matchingSub?.subscriptionId ?: (index + 1)
                    val displayName = matchingSub?.displayName?.toString()
                        ?: phoneAccount?.label?.toString()
                        ?: "SIM ${slotIndex + 1}"
                    val carrierName = matchingSub?.carrierName?.toString()
                        ?: phoneAccount?.shortDescription?.toString()
                        ?: displayName

                    registeredAccounts.add(
                        SimAccountInfo(
                            slotIndex = slotIndex,
                            subscriptionId = subId,
                            displayName = displayName,
                            carrierName = carrierName,
                            phoneAccountHandle = handle
                        )
                    )
                }
            } else if (!subscriptions.isNullOrEmpty()) {
                val defaultHandle = telecomManager.getDefaultOutgoingPhoneAccount(PhoneAccount.SCHEME_TEL)
                if (defaultHandle != null) {
                    subscriptions.forEach { subInfo ->
                        val label = subInfo.displayName?.toString() ?: "SIM ${subInfo.simSlotIndex + 1}"
                        val carrier = subInfo.carrierName?.toString() ?: "Cellular"
                        registeredAccounts.add(
                            SimAccountInfo(
                                slotIndex = subInfo.simSlotIndex,
                                subscriptionId = subInfo.subscriptionId,
                                displayName = label,
                                carrierName = carrier,
                                phoneAccountHandle = defaultHandle
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return registeredAccounts
    }
}
