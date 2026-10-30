package com.ryanshelby.linea.mergefix

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MergeUndoReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MergeUndoEntryPoint {
        fun mergeExecutor(): ContactMergeExecutor
    }

    override fun onReceive(context: Context, intent: Intent) {
        val historyId = intent.getLongExtra(EXTRA_HISTORY_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 9001)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(notificationId)

        if (historyId <= 0) return

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            MergeUndoEntryPoint::class.java
        )
        val executor = entryPoint.mergeExecutor()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                executor.unlinkMerge(historyId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val ACTION_UNDO_MERGE = "com.ryanshelby.linea.action.UNDO_MERGE"
        const val EXTRA_HISTORY_ID = "extra_history_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}

