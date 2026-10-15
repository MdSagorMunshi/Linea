package com.ryanshelby.linea.telecom

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

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == "android.telecom.action.SHOW_MISSED_CALLS_NOTIFICATION" ||
            action == TelecomManager.ACTION_SHOW_MISSED_CALLS_NOTIFICATION) {

            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            try {
                telecomManager?.cancelMissedCallsNotification()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        telecomManager?.cancelMissedCallsNotification()
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

            // Fallback for missed calls where LineaInCallService was not running
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val lookup = contactLookupHelper.lookupContact(phoneNumber)
                    notificationManager.showMissedCallNotification(
                        callerName = lookup.displayName,
                        phoneNumber = phoneNumber,
                        simSlot = 0,
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
