package com.ryanshelby.linea.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.TelecomManager
import com.ryanshelby.linea.notifications.CallNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Handles the Telecom platform's ACTION_SHOW_MISSED_CALLS_NOTIFICATION broadcast,
 * which is delivered exclusively to the default dialer when a missed call occurs.
 *
 * It immediately clears the stock Android system notification and ensures LINEA
 * posts its dedicated, branded missed-call notification instead.
 */
@AndroidEntryPoint
class MissedCallReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationManager: CallNotificationManager

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
            android.util.Log.d("MissedCallReceiver", "Received SHOW_MISSED_CALLS_NOTIFICATION for: $phoneNumber")

            if (phoneNumber.isNotBlank()) {
                notificationManager.showMissedCallNotification(
                    callerName = null,
                    phoneNumber = phoneNumber,
                    simSlot = 0,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }
}
