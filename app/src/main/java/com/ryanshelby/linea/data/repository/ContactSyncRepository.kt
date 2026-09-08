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
        // 1. Load and group contacts from Android ContactsContract by Contact ID
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
        )

        data class RawPhone(val number: String, val label: String)
        data class GroupedContact(
            val androidId: Long,
            val displayName: String,
            val photoUri: String?,
            val phones: MutableList<RawPhone>
        )

        val groupedMap = linkedMapOf<String, GroupedContact>()
        val t9List = mutableListOf<T9Contact>()

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
                val typeIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val labelIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)

                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getLong(idIdx) else 0L
                    val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "Unknown" else "Unknown"
                    val number = if (numberIdx >= 0) it.getString(numberIdx) ?: "" else ""
                    val photo = if (photoIdx >= 0) it.getString(photoIdx) else null
                    val type = if (typeIdx >= 0) it.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    val customLabel = if (labelIdx >= 0) it.getString(labelIdx) else null

                    if (number.isNotBlank()) {
                        val label = when (type) {
                            ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                            ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
                            ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> customLabel ?: "Custom"
                            else -> "Mobile"
                        }

                        t9List.add(
                            T9Contact(
                                id = id,
                                displayName = name,
                                phoneNumber = number,
                                photoUri = photo
                            )
                        )

                        // Key by Android Contact ID, or fallback to trimmed display name
                        val key = if (id > 0) "id_$id" else "name_${name.trim().lowercase()}"
                        val existing = groupedMap.getOrPut(key) {
                            GroupedContact(
                                androidId = id,
                                displayName = name,
                                photoUri = photo,
                                phones = mutableListOf()
                            )
                        }
                        if (existing.phones.none { it.number == number }) {
                            existing.phones.add(RawPhone(number, label))
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        _cachedT9Contacts.value = t9List

        // 2. Synchronize grouped contacts into Room database
        for ((_, grouped) in groupedMap) {
            val existing = if (grouped.androidId > 0) {
                contactDao.getContactByAndroidId(grouped.androidId) ?: contactDao.getContactByName(grouped.displayName)
            } else {
                contactDao.getContactByName(grouped.displayName)
            }

            val targetContactId: Long
            if (existing != null) {
                targetContactId = existing.id
                contactDao.updateContact(
                    existing.copy(
                        androidContactId = grouped.androidId,
                        displayName = grouped.displayName,
                        photoUri = grouped.photoUri ?: existing.photoUri
                    )
                )
            } else {
                val newEntity = ContactEntity(
                    androidContactId = grouped.androidId,
                    displayName = grouped.displayName,
                    photoUri = grouped.photoUri
                )
                targetContactId = contactDao.insertContact(newEntity)
            }

            // Insert distinct phone numbers for this single contact
            contactDao.deleteNumbersForContact(targetContactId)
            val numberEntities = grouped.phones.mapIndexed { index, phone ->
                ContactNumberEntity(
                    contactId = targetContactId,
                    number = phone.number,
                    normalizedNumber = phone.number.filter { it.isDigit() || it == '+' },
                    label = phone.label,
                    isPrimary = index == 0
                )
            }
            if (numberEntities.isNotEmpty()) {
                contactDao.insertNumbers(numberEntities)
            }
        }

        // 3. Clean up any leftover duplicate contacts in the Room database
        deduplicateLocalContacts()
    }

    suspend fun deduplicateLocalContacts() = withContext(Dispatchers.IO) {
        try {
            val allContacts = contactDao.getAllContactsOnce()
            // Group by trimmed lowercase display name or androidContactId
            val groupedByName = allContacts.groupBy { it.displayName.trim().lowercase() }
            for ((_, list) in groupedByName) {
                if (list.size > 1) {
                    // Keep the primary contact (favorite or lowest ID)
                    val primary = list.firstOrNull { it.isFavorite } ?: list.minByOrNull { it.id } ?: list.first()
                    val duplicates = list.filter { it.id != primary.id }
                    for (dup in duplicates) {
                        contactDao.reassignNumbersContact(dup.id, primary.id)
                        contactDao.deleteContactById(dup.id)
                    }

                    // Remove any duplicate numbers assigned to primary
                    val numbers = contactDao.getNumbersForContactOnce(primary.id)
                    val distinctNumbers = numbers.distinctBy { it.normalizedNumber }
                    if (distinctNumbers.size < numbers.size) {
                        contactDao.deleteNumbersForContact(primary.id)
                        contactDao.insertNumbers(distinctNumbers)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
