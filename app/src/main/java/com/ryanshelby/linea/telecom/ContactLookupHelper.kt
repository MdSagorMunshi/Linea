package com.ryanshelby.linea.telecom

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.ryanshelby.linea.data.local.dao.ContactDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class ContactLookupResult(
    val displayName: String?,
    val photoUri: String? = null,
    val customRingtoneUri: String? = null,
    val isPrivate: Boolean = false
)

@Singleton
class ContactLookupHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactDao: ContactDao
) {
    private val cache = ConcurrentHashMap<String, ContactLookupResult>()

    suspend fun lookupContact(phoneNumber: String): ContactLookupResult = withContext(Dispatchers.IO) {
        val trimmedNumber = phoneNumber.trim()
        if (trimmedNumber.isBlank()) {
            return@withContext ContactLookupResult(displayName = null)
        }

        cache[trimmedNumber]?.let { return@withContext it }

        var resolvedName: String? = null
        var resolvedPhoto: String? = null
        var resolvedRingtone: String? = null

        // 1. Query Android System ContactsContract.PhoneLookup
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(trimmedNumber)
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.PHOTO_URI,
                ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI,
                ContactsContract.PhoneLookup.CUSTOM_RINGTONE
            )

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    val photoIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
                    val thumbIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)
                    val ringtoneIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.CUSTOM_RINGTONE)

                    if (nameIdx >= 0) {
                        resolvedName = cursor.getString(nameIdx)
                    }
                    if (photoIdx >= 0) {
                        resolvedPhoto = cursor.getString(photoIdx)
                    }
                    if (resolvedPhoto.isNullOrBlank() && thumbIdx >= 0) {
                        resolvedPhoto = cursor.getString(thumbIdx)
                    }
                    if (ringtoneIdx >= 0) {
                        resolvedRingtone = cursor.getString(ringtoneIdx)
                    }
                }
            }
        } catch (e: SecurityException) {
            // READ_CONTACTS permission might not be granted yet
        } catch (e: Exception) {
            e.printStackTrace()
        }

        var isPrivate = false

        // 2. Query Local Room ContactDao if name not resolved or to check local ringtone/contact settings
        try {
            val digitsOnly = trimmedNumber.filter { it.isDigit() }
            if (digitsOnly.isNotBlank()) {
                val localNumber = contactDao.findNumberByNormalized(digitsOnly)
                    ?: if (digitsOnly.length > 7) {
                        // Match last 7-9 digits for local formatting differences
                        val suffix = digitsOnly.takeLast(7)
                        contactDao.findNumberByNormalized(suffix)
                    } else null

                if (localNumber != null) {
                    val localContact = contactDao.getContactById(localNumber.contactId)
                    if (localContact != null) {
                        if (localContact.isPrivate) {
                            isPrivate = true
                            resolvedName = localContact.displayName
                        } else if (resolvedName.isNullOrBlank()) {
                            resolvedName = localContact.displayName
                        }
                        if (resolvedPhoto.isNullOrBlank()) {
                            resolvedPhoto = localContact.photoUri
                        }
                        if (resolvedRingtone.isNullOrBlank()) {
                            resolvedRingtone = localContact.customRingtoneUri
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val result = ContactLookupResult(
            displayName = resolvedName,
            photoUri = resolvedPhoto,
            customRingtoneUri = resolvedRingtone,
            isPrivate = isPrivate
        )

        if (resolvedName != null) {
            cache[trimmedNumber] = result
        }

        result
    }

    fun clearCache() {
        cache.clear()
    }
}
