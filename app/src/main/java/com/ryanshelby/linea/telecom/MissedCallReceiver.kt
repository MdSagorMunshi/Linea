package com.ryanshelby.linea.telecom

import com.ryanshelby.linea.telecom.TelecomRoleActions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.TelecomManager
import com.ryanshelby.linea.notifications.CallNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles the Telecom platform's ACTION_SHOW_MISSED_CALLS_NOTIFICATION broadcast,
 * which is delivered exclusively to the default dialer when a missed call occurs.
 *
 * It immediately clears the stock Android system notification and ensures LINEA
 * posts its dedicated, branded missed-call notification instead.
 *
 * If LINEA's InCallService already handled and posted the notification via CallManager,
 * this receiver suppresses the broadcast to prevent duplicate notifications.
 */
@AndroidEntryPoint
class MissedCallReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationManager: CallNotificationManager

    @Inject
    lateinit var contactLookupHelper: ContactLookupHelper

    @Inject
    lateinit var simAccountResolver: SimAccountResolver

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == "android.telecom.action.SHOW_MISSED_CALLS_NOTIFICATION" ||
            action == TelecomManager.ACTION_SHOW_MISSED_CALLS_NOTIFICATION) {

            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            try {
                TelecomRoleActions.cancelMissedCalls(context, telecomManager)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        TelecomRoleActions.cancelMissedCalls(context, telecomManager)
                    } catch (_: Exception) {}
                }, 600)
            } catch (e: Exception) {
                android.util.Log.e("MissedCallReceiver", "Failed to cancel system missed call notification", e)
            }

            val phoneNumber = intent.getStringExtra(TelecomManager.EXTRA_NOTIFICATION_PHONE_NUMBER) ?: ""
            android.util.Log.d("MissedCallReceiver", "Received SHOW_MISSED_CALLS_NOTIFICATION for: '$phoneNumber'")

            if (phoneNumber.isBlank()) {
                return
            }

            // If CallManager already handled and notified this missed call via InCallService, suppress duplicate!
            if (notificationManager.hasRecentMissedCallNotification(phoneNumber)) {
                android.util.Log.d("MissedCallReceiver", "Missed call for '$phoneNumber' already notified by CallManager. Suppressing duplicate.")
                return
            }

            // Extract PhoneAccountHandle if present in intent extras
            val phoneAccountHandle: android.telecom.PhoneAccountHandle? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, android.telecom.PhoneAccountHandle::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE)
            }

            // Fallback for missed calls where LineaInCallService was not running
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    var resolvedSlot: Int? = simAccountResolver.resolveSimSlot(phoneAccountHandle)

                    // If slot not resolved from intent extra, query system call log defensively
                    if (resolvedSlot == null) {
                        try {
                            val cursor = context.contentResolver.query(
                                android.provider.CallLog.Calls.CONTENT_URI,
                                arrayOf(
                                    android.provider.CallLog.Calls.PHONE_ACCOUNT_ID,
                                    android.provider.CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME
                                ),
                                "${android.provider.CallLog.Calls.TYPE} = ? AND ${android.provider.CallLog.Calls.NUMBER} = ?",
                                arrayOf(android.provider.CallLog.Calls.MISSED_TYPE.toString(), phoneNumber),
                                "${android.provider.CallLog.Calls.DATE} DESC LIMIT 1"
                            )
                            cursor?.use {
                                if (it.moveToFirst()) {
                                    val accId = it.getString(0)
                                    val compName = it.getString(1)
                                    resolvedSlot = simAccountResolver.resolveSimSlotFromCallLog(accId, compName)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("MissedCallReceiver", "Failed to query system call log for missed call account info", e)
                        }
                    }

                    val lookup = contactLookupHelper.lookupContact(phoneNumber)
                    notificationManager.showMissedCallNotification(
                        callerName = lookup.displayName,
                        phoneNumber = phoneNumber,
                        simSlot = resolvedSlot ?: -1,
                        timestamp = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    android.util.Log.e("MissedCallReceiver", "Error handling fallback missed call notification", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
