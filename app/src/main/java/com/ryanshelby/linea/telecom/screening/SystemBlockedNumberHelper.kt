package com.ryanshelby.linea.telecom.screening

import android.content.ContentValues
import android.content.Context
import android.provider.BlockedNumberContract
import android.util.Log

/**
 * Helper to sync blocked numbers with Android's system BlockedNumberContract.
 * When an app is the Default Dialer (ROLE_DIALER), adding numbers to
 * BlockedNumberContract ensures the OS Telecom stack blocks the caller at framework level.
 */
object SystemBlockedNumberHelper {
    private const val TAG = "SystemBlockedNumber"

    fun canCurrentUserBlockNumbers(context: Context): Boolean {
        return try {
            BlockedNumberContract.canCurrentUserBlockNumbers(context)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot check canCurrentUserBlockNumbers: ${e.message}")
            false
        }
    }

    fun blockNumber(context: Context, number: String) {
        val cleanNumber = number.trim()
        if (cleanNumber.isBlank()) return
        try {
            if (!canCurrentUserBlockNumbers(context)) return
            if (isBlocked(context, cleanNumber)) return

            val values = ContentValues().apply {
                put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, cleanNumber)
            }
            context.contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values)
            Log.d(TAG, "Successfully synced blocked number to Android system: $cleanNumber")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync blocked number to Android system: ${e.message}")
        }
    }

    fun unblockNumber(context: Context, number: String) {
        val cleanNumber = number.trim()
        if (cleanNumber.isBlank()) return
        try {
            if (!canCurrentUserBlockNumbers(context)) return
            BlockedNumberContract.unblock(context, cleanNumber)
            Log.d(TAG, "Successfully unblocked number in Android system: $cleanNumber")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unblock number in Android system: ${e.message}")
        }
    }

    fun isBlocked(context: Context, number: String): Boolean {
        val cleanNumber = number.trim()
        if (cleanNumber.isBlank()) return false
        return try {
            if (!canCurrentUserBlockNumbers(context)) return false
            BlockedNumberContract.isBlocked(context, cleanNumber)
        } catch (e: Exception) {
            false
        }
    }
}
