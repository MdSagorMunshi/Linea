package com.ryanshelby.linea.telecom

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import com.ryanshelby.linea.R
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

    private val connectionServiceComponent = ComponentName(context, LineaConnectionService::class.java)

    @SuppressLint("MissingPermission")
    fun registerPhoneAccounts(): List<SimAccountInfo> {
        val registeredAccounts = mutableListOf<SimAccountInfo>()
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java) ?: return emptyList()

        try {
            val subscriptions: List<SubscriptionInfo>? = subscriptionManager.activeSubscriptionInfoList
            if (!subscriptions.isNullOrEmpty()) {
                subscriptions.forEach { subInfo ->
                    val handleId = "linea_sim_${subInfo.subscriptionId}"
                    val handle = PhoneAccountHandle(connectionServiceComponent, handleId)
                    val label = subInfo.displayName?.toString() ?: "SIM ${subInfo.simSlotIndex + 1}"
                    val carrier = subInfo.carrierName?.toString() ?: "Cellular"

                    val phoneAccount = PhoneAccount.builder(handle, label)
                        .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER or PhoneAccount.CAPABILITY_CONNECTION_MANAGER)
                        .setIcon(Icon.createWithResource(context, R.drawable.ic_launcher_foreground))
                        .setShortDescription(carrier)
                        .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
                        .build()

                    telecomManager.registerPhoneAccount(phoneAccount)
                    registeredAccounts.add(
                        SimAccountInfo(
                            slotIndex = subInfo.simSlotIndex,
                            subscriptionId = subInfo.subscriptionId,
                            displayName = label,
                            carrierName = carrier,
                            phoneAccountHandle = handle
                        )
                    )
                }
            } else {
                // Fallback virtual account for devices/emulators with no active physical SIM
                val defaultHandle = PhoneAccountHandle(connectionServiceComponent, "linea_default_sim")
                val defaultAccount = PhoneAccount.builder(defaultHandle, "LINEA Cellular")
                    .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER or PhoneAccount.CAPABILITY_CONNECTION_MANAGER)
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_launcher_foreground))
                    .setShortDescription("Cellular Call Provider")
                    .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
                    .build()

                telecomManager.registerPhoneAccount(defaultAccount)
                registeredAccounts.add(
                    SimAccountInfo(
                        slotIndex = 0,
                        subscriptionId = -1,
                        displayName = "Cellular",
                        carrierName = "Default Carrier",
                        phoneAccountHandle = defaultHandle
                    )
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        return registeredAccounts
    }
}
