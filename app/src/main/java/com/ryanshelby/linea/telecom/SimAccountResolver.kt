package com.ryanshelby.linea.telecom

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telecom.Call
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimAccountResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telecomManager: TelecomManager,
    private val telephonyManager: TelephonyManager,
    private val subscriptionManager: SubscriptionManager
) {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val _simAccounts = MutableStateFlow<List<SimAccountInfo>>(emptyList())
    val simAccounts: StateFlow<List<SimAccountInfo>> = _simAccounts.asStateFlow()

    private val _hasMultipleSims = MutableStateFlow(false)
    val hasMultipleSims: StateFlow<Boolean> = _hasMultipleSims.asStateFlow()

    private val _activeSimCount = MutableStateFlow(0)
    val activeSimCount: StateFlow<Int> = _activeSimCount.asStateFlow()

    private val subscriptionsChangedListener = object : SubscriptionManager.OnSubscriptionsChangedListener() {
        override fun onSubscriptionsChanged() {
            android.util.Log.d("SimAccountResolver", "Subscriptions changed callback triggered")
            scope.launch {
                refresh()
            }
        }
    }

    private val simStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val action = intent?.action
            android.util.Log.d("SimAccountResolver", "SIM broadcast received: $action")
            scope.launch {
                refresh()
            }
        }
    }

    init {
        registerListeners()
        refresh()
    }

    private fun registerListeners() {
        try {
            subscriptionManager.addOnSubscriptionsChangedListener(
                context.mainExecutor,
                subscriptionsChangedListener
            )
        } catch (_: Exception) {
            try {
                @Suppress("DEPRECATION")
                subscriptionManager.addOnSubscriptionsChangedListener(subscriptionsChangedListener)
            } catch (_: Exception) {}
        }

        try {
            val filter = IntentFilter().apply {
                addAction("android.intent.action.SIM_STATE_CHANGED")
                addAction("android.telephony.action.SIM_CARD_STATE_CHANGED")
                addAction("android.telephony.action.SIM_APPLICATION_STATE_CHANGED")
                addAction("android.telephony.action.DEFAULT_SUBSCRIPTION_CHANGED")
                addAction("android.telephony.action.DEFAULT_SMS_SUBSCRIPTION_CHANGED")
            }
            ContextCompat.registerReceiver(
                context,
                simStateReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) {
            android.util.Log.w("SimAccountResolver", "Failed to register SIM state receiver", e)
        }
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    fun refresh(): List<SimAccountInfo> {
        val subscriptions: List<SubscriptionInfo> = try {
            subscriptionManager.activeSubscriptionInfoList.orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val callCapableHandles: List<PhoneAccountHandle> = try {
            telecomManager.callCapablePhoneAccounts.orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        // Map call-capable handles by subscriptionId using official TelephonyManager API
        val handleBySubId = mutableMapOf<Int, PhoneAccountHandle>()
        for (handle in callCapableHandles) {
            val subId = try {
                telephonyManager.getSubscriptionId(handle)
            } catch (_: Exception) {
                SubscriptionManager.INVALID_SUBSCRIPTION_ID
            }
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                handleBySubId[subId] = handle
            }
        }

        val result = mutableListOf<SimAccountInfo>()

        // 1. Process active subscriptions ordered by slot index
        val sortedSubs = subscriptions.sortedBy { it.simSlotIndex }
        for (sub in sortedSubs) {
            var handle = handleBySubId[sub.subscriptionId]
            if (handle == null) {
                // Defensive fallback: match against callCapableHandles by ID or ICCID
                handle = callCapableHandles.firstOrNull { h ->
                    val id = h.id ?: ""
                    id == sub.subscriptionId.toString() ||
                    (!sub.iccId.isNullOrBlank() && (id.equals(sub.iccId, ignoreCase = true) || id.contains(sub.iccId))) ||
                    (sub.cardId >= 0 && id == sub.cardId.toString())
                }
            }

            if (handle != null) {
                val phoneAccount = try { telecomManager.getPhoneAccount(handle) } catch (_: Exception) { null }
                val displayName = sub.displayName?.toString()?.ifBlank { null }
                    ?: phoneAccount?.label?.toString()?.ifBlank { null }
                    ?: "SIM ${sub.simSlotIndex + 1}"
                val carrierName = sub.carrierName?.toString()?.ifBlank { null }
                    ?: phoneAccount?.shortDescription?.toString()?.ifBlank { null }
                    ?: displayName

                result.add(
                    SimAccountInfo(
                        slotIndex = sub.simSlotIndex,
                        subscriptionId = sub.subscriptionId,
                        displayName = displayName,
                        carrierName = carrierName,
                        phoneAccountHandle = handle
                    )
                )
            }
        }


        _simAccounts.value = result
        val activeCount = result.map { it.slotIndex }.distinct().count { it in 0..1 }
        _activeSimCount.value = activeCount
        _hasMultipleSims.value = activeCount > 1

        android.util.Log.d("SimAccountResolver", "Refreshed SIMs: count=$activeCount, multiple=${activeCount > 1}, accounts=${result.map { "${it.displayName}(slot=${it.slotIndex},subId=${it.subscriptionId})" }}")
        return result
    }

    /**
     * Resolves a PhoneAccountHandle to its 0-based physical/logical SIM slot index.
     * Returns null if it cannot be resolved, or if it is an emergency or non-cellular account.
     */
    @SuppressLint("MissingPermission")
    fun resolveSimSlot(handle: PhoneAccountHandle?): Int? {
        if (handle == null) return null

        // 1. Check cached SIM accounts first
        val cached = _simAccounts.value.find {
            it.phoneAccountHandle == handle ||
            (it.phoneAccountHandle.componentName == handle.componentName && it.phoneAccountHandle.id == handle.id)
        }
        if (cached != null && cached.slotIndex in 0..1) {
            return cached.slotIndex
        }

        // 2. Official TelephonyManager API: getSubscriptionId(handle)
        val subId = try {
            telephonyManager.getSubscriptionId(handle)
        } catch (_: SecurityException) {
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        } catch (_: Exception) {
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        }

        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            val subInfo = try {
                subscriptionManager.getActiveSubscriptionInfo(subId)
            } catch (_: Exception) { null }
            if (subInfo != null && subInfo.simSlotIndex in 0..1) {
                return subInfo.simSlotIndex
            }
            try {
                val list = subscriptionManager.activeSubscriptionInfoList
                val match = list?.find { it.subscriptionId == subId }
                if (match != null && match.simSlotIndex in 0..1) {
                    return match.simSlotIndex
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback: match handle.id against active subscriptions (subId, ICCID, cardId)
        val handleId = handle.id
        if (!handleId.isNullOrBlank()) {
            try {
                val list = subscriptionManager.activeSubscriptionInfoList
                if (list != null) {
                    for (sub in list) {
                        if (handleId == sub.subscriptionId.toString() ||
                            (!sub.iccId.isNullOrBlank() && (handleId.equals(sub.iccId, ignoreCase = true) || handleId.contains(sub.iccId))) ||
                            (sub.cardId >= 0 && handleId == sub.cardId.toString())
                        ) {
                            if (sub.simSlotIndex in 0..1) {
                                return sub.simSlotIndex
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 4. Fallback: Check PhoneAccount extras for subscription ID
        try {
            val phoneAccount = telecomManager.getPhoneAccount(handle)
            val extras = phoneAccount?.extras
            if (extras != null) {
                val extraSubId = extras.getInt("android.telecom.extra.SUBSCRIPTION_ID", SubscriptionManager.INVALID_SUBSCRIPTION_ID)
                if (SubscriptionManager.isValidSubscriptionId(extraSubId)) {
                    val subInfo = subscriptionManager.getActiveSubscriptionInfo(extraSubId)
                    if (subInfo != null && subInfo.simSlotIndex in 0..1) {
                        return subInfo.simSlotIndex
                    }
                }
            }
        } catch (_: Exception) {}

        // Never guess or default to 0!
        return null
    }

    /**
     * Resolves the SIM slot for an active or disconnected Call.
     * Takes emergency calls into account (which have no specific SIM).
     */
    fun resolveSimSlotFromCall(call: Call?): Int? {
        if (call == null) return null
        if (isEmergencyCall(call, call.details?.handle?.schemeSpecificPart)) {
            return null
        }
        return resolveSimSlot(call.details?.accountHandle)
    }

    /**
     * Resolves a CallLog record to its 0-based SIM slot index using the stored
     * phone account ID and component name.
     */
    @SuppressLint("MissingPermission")
    fun resolveSimSlotFromCallLog(phoneAccountId: String?, componentName: String?): Int? {
        if (phoneAccountId.isNullOrBlank()) return null

        // 1. Try reconstructing PhoneAccountHandle if componentName is present
        if (!componentName.isNullOrBlank()) {
            val cmp = try {
                ComponentName.unflattenFromString(componentName)
            } catch (_: Exception) { null }
            if (cmp != null) {
                val handle = PhoneAccountHandle(cmp, phoneAccountId)
                val resolved = resolveSimSlot(handle)
                if (resolved != null) return resolved
            }
        }

        // 2. Match against cached SIM accounts
        val cached = _simAccounts.value.find {
            it.phoneAccountHandle.id == phoneAccountId ||
            it.subscriptionId.toString() == phoneAccountId
        }
        if (cached != null && cached.slotIndex in 0..1) {
            return cached.slotIndex
        }

        // 3. Match against active subscriptions (subId, ICCID, cardId)
        try {
            val list = subscriptionManager.activeSubscriptionInfoList
            if (list != null) {
                for (sub in list) {
                    if (phoneAccountId == sub.subscriptionId.toString() ||
                        (!sub.iccId.isNullOrBlank() && (phoneAccountId.equals(sub.iccId, ignoreCase = true) || phoneAccountId.contains(sub.iccId))) ||
                        (sub.cardId >= 0 && phoneAccountId == sub.cardId.toString())
                    ) {
                        if (sub.simSlotIndex in 0..1) {
                            return sub.simSlotIndex
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 4. Match against call capable handles by ID
        try {
            val handles = telecomManager.callCapablePhoneAccounts.orEmpty()
            for (h in handles) {
                if (h.id == phoneAccountId) {
                    val resolved = resolveSimSlot(h)
                    if (resolved != null) return resolved
                }
            }
        } catch (_: Exception) {}

        // 5. Some OEMs write subId directly as integer into phoneAccountId
        val parsedSubId = phoneAccountId.toIntOrNull()
        if (parsedSubId != null && SubscriptionManager.isValidSubscriptionId(parsedSubId)) {
            try {
                val sub = subscriptionManager.getActiveSubscriptionInfo(parsedSubId)
                if (sub != null && sub.simSlotIndex in 0..1) {
                    return sub.simSlotIndex
                }
            } catch (_: Exception) {}
        }

        return null
    }

    /**
     * Returns the user-facing display label ("SIM 1" or "SIM 2").
     * Returns null if single-SIM device (unless forceShow is true) or if slot is invalid.
     */
    fun getSimLabel(slotIndex: Int?, forceShow: Boolean = false): String? {
        if (slotIndex == null || slotIndex !in 0..1) return null
        if (!forceShow && !_hasMultipleSims.value) return null
        return "SIM ${slotIndex + 1}"
    }

    /**
     * Returns the carrier/display name if available, or fallback to "SIM 1"/"SIM 2".
     */
    fun getSimDisplayName(slotIndex: Int?): String? {
        if (slotIndex == null || slotIndex !in 0..1) return null
        val account = _simAccounts.value.find { it.slotIndex == slotIndex }
        return account?.displayName ?: "SIM ${slotIndex + 1}"
    }

    /**
     * Checks if a call or phone number is an emergency call.
     */
    fun isEmergencyCall(call: Call?, number: String?): Boolean {
        if (number != null && (PhoneNumberUtils.isEmergencyNumber(number) || number == "911" || number == "112" || number == "999" || number == "000")) {
            return true
        }
        if (call != null) {
            val details = call.details
            if (details != null && details.hasProperty(Call.Details.PROPERTY_EMERGENCY_CALLBACK_MODE)) {
                return true
            }
        }
        return false
    }

    /**
     * Returns the PhoneAccountHandle for a given SIM slot index (0 or 1).
     */
    fun getPhoneAccountHandleForSlot(slotIndex: Int): PhoneAccountHandle? {
        return _simAccounts.value.find { it.slotIndex == slotIndex }?.phoneAccountHandle
    }
}
