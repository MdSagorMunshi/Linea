package com.ryanshelby.linea.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.provider.ContactsContract
import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.local.entities.ContactNumberEntity
import com.ryanshelby.linea.telecom.T9Contact
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactSyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactDao: ContactDao
) {

    private val _cachedT9Contacts = MutableStateFlow<List<T9Contact>>(emptyList())
    val cachedT9Contacts: StateFlow<List<T9Contact>> = _cachedT9Contacts.asStateFlow()

    @SuppressLint("Range")
    suspend fun loadContacts(): List<T9Contact> = withContext(Dispatchers.IO) {
        val contactsList = mutableListOf<T9Contact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        )

        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getLong(idIdx) else 0L
                    val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "Unknown" else "Unknown"
                    val number = if (numberIdx >= 0) it.getString(numberIdx) ?: "" else ""
                    val photo = if (photoIdx >= 0) it.getString(photoIdx) else null

                    if (number.isNotBlank()) {
                        contactsList.add(
                            T9Contact(
                                id = id,
                                displayName = name,
                                phoneNumber = number,
                                photoUri = photo
                            )
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        _cachedT9Contacts.value = contactsList
        contactsList
    }

    suspend fun syncWithLocalDb() = withContext(Dispatchers.IO) {
        val contacts = loadContacts()
        for (c in contacts) {
            val contactEntity = ContactEntity(
                androidContactId = c.id,
                displayName = c.displayName,
                photoUri = c.photoUri
            )
            val insertedId = contactDao.insertContact(contactEntity)
            val numberEntity = ContactNumberEntity(
                contactId = insertedId,
                number = c.phoneNumber,
                normalizedNumber = c.phoneNumber.filter { it.isDigit() || it == '+' },
                label = "Mobile",
                isPrimary = true
            )
            contactDao.insertNumbers(listOf(numberEntity))
        }
    }

    suspend fun createContact(
        displayName: String,
        company: String? = null,
        numbers: List<Pair<String, String>>, // (Number, Label)
        emails: List<String> = emptyList(),
        preferredSimSlot: Int? = null,
        notes: String? = null
    ): Long = withContext(Dispatchers.IO) {
        var rawContactId: Long? = null

        // 1. Dual-write to Android's ContactsContract
        try {
            val ops = ArrayList<android.content.ContentProviderOperation>()
            val rawContactInsertIndex = ops.size

            ops.add(
                android.content.ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            // Name
            ops.add(
                android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                    .build()
            )

            // Numbers
            for ((num, label) in numbers) {
                if (num.isNotBlank()) {
                    val phoneType = when (label.lowercase()) {
                        "work" -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
                        "home" -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
                        else -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    }
                    ops.add(
                        android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, num)
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType)
                            .build()
                    )
                }
            }

            val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            if (results.isNotEmpty() && results[0].uri != null) {
                rawContactId = android.content.ContentUris.parseId(results[0].uri!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Insert into LINEA local Room database
        val contactEntity = ContactEntity(
            androidContactId = rawContactId,
            displayName = displayName,
            company = company,
            preferredSimSlot = preferredSimSlot,
            notes = notes
        )
        val contactId = contactDao.insertContact(contactEntity)

        val numberEntities = numbers.mapIndexed { idx, (num, label) ->
            ContactNumberEntity(
                contactId = contactId,
                number = num,
                normalizedNumber = num.filter { it.isDigit() || it == '+' },
                label = label,
                isPrimary = idx == 0
            )
        }
        contactDao.insertNumbers(numberEntities)

        val emailEntities = emails.filter { it.isNotBlank() }.map { email ->
            com.ryanshelby.linea.data.local.entities.ContactEmailEntity(
                contactId = contactId,
                email = email,
                label = "Home"
            )
        }
        if (emailEntities.isNotEmpty()) {
            contactDao.insertEmails(emailEntities)
        }

        // Refresh cached T9 contacts
        loadContacts()

        contactId
    }

    suspend fun updateContact(contact: ContactEntity) = withContext(Dispatchers.IO) {
        contactDao.updateContact(contact)
        loadContacts()
    }

    suspend fun deleteContact(id: Long, androidContactId: Long? = null) = withContext(Dispatchers.IO) {
        contactDao.deleteContactById(id)
        if (androidContactId != null) {
            try {
                val uri = android.content.ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, androidContactId)
                context.contentResolver.delete(uri, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        loadContacts()
    }
}
