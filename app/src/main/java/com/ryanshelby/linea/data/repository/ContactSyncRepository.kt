package com.ryanshelby.linea.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.entities.ContactEmailEntity
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

data class ContactAccount(
    val name: String,
    val type: String?,
    val isDevice: Boolean = (type == null)
)

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
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
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
                val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val thumbIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getLong(idIdx) else 0L
                    val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "Unknown" else "Unknown"
                    val number = if (numberIdx >= 0) it.getString(numberIdx) ?: "" else ""
                    val highResPhoto = if (photoIdx >= 0) it.getString(photoIdx) else null
                    val thumbPhoto = if (thumbIdx >= 0) it.getString(thumbIdx) else null
                    val resolvedPhoto = highResPhoto ?: thumbPhoto

                    if (number.isNotBlank()) {
                        contactsList.add(
                            T9Contact(
                                id = id,
                                displayName = name,
                                phoneNumber = number,
                                photoUri = resolvedPhoto
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
            ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
        )

        data class RawPhone(val number: String, val label: String)
        data class GroupedContact(
            val androidId: Long,
            val rawContactId: Long,
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
                val rawIdIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val thumbIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                val typeIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val labelIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)

                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getLong(idIdx) else 0L
                    val rawId = if (rawIdIdx >= 0) it.getLong(rawIdIdx) else 0L
                    val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "Unknown" else "Unknown"
                    val number = if (numberIdx >= 0) it.getString(numberIdx) ?: "" else ""
                    val highResPhoto = if (photoIdx >= 0) it.getString(photoIdx) else null
                    val thumbPhoto = if (thumbIdx >= 0) it.getString(thumbIdx) else null
                    val resolvedPhoto = highResPhoto ?: thumbPhoto
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
                                photoUri = resolvedPhoto
                            )
                        )

                        // Key by Android Contact ID, or fallback to trimmed display name
                        val key = if (id > 0) "id_$id" else "name_${name.trim().lowercase()}"
                        val existing = groupedMap.getOrPut(key) {
                            GroupedContact(
                                androidId = id,
                                rawContactId = rawId,
                                displayName = name,
                                photoUri = resolvedPhoto,
                                phones = mutableListOf()
                            )
                        }
                        if (existing.photoUri.isNullOrBlank() && !resolvedPhoto.isNullOrBlank()) {
                            groupedMap[key] = existing.copy(photoUri = resolvedPhoto)
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
                contactDao.getContactByAndroidId(grouped.androidId)
                    ?: (if (grouped.rawContactId > 0) contactDao.getContactByAndroidId(grouped.rawContactId) else null)
                    ?: contactDao.getContactByName(grouped.displayName)
            } else {
                contactDao.getContactByName(grouped.displayName)
            }

            // CRITICAL: If an existing contact is in the Private Safe, never overwrite or link it from external Android ContactsContract
            if (existing != null && existing.isPrivate) {
                continue
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

        // 3. Clean up any leftover duplicate contacts in the Room database (excluding private contacts)
        deduplicateLocalContacts()
    }

    suspend fun deduplicateLocalContacts() = withContext(Dispatchers.IO) {
        try {
            val allContacts = contactDao.getAllContactsOnce().filter { !it.isPrivate }
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

    suspend fun getAvailableAccounts(): List<ContactAccount> = withContext(Dispatchers.IO) {
        val accounts = linkedSetOf<ContactAccount>()

        // 1. Check AccountManager for accounts
        try {
            val accountManager = android.accounts.AccountManager.get(context)
            val googleAccounts = accountManager.getAccountsByType("com.google")
            for (acc in googleAccounts) {
                if (!acc.name.isNullOrBlank()) {
                    accounts.add(ContactAccount(name = acc.name, type = acc.type, isDevice = false))
                }
            }
            val allAccounts = accountManager.accounts
            for (acc in allAccounts) {
                if (!acc.name.isNullOrBlank()) {
                    accounts.add(ContactAccount(name = acc.name, type = acc.type, isDevice = false))
                }
            }
        } catch (_: Exception) {}

        // 2. Query RawContacts for existing sync accounts
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts.ACCOUNT_NAME, ContactsContract.RawContacts.ACCOUNT_TYPE),
                "${ContactsContract.RawContacts.ACCOUNT_NAME} IS NOT NULL",
                null,
                null
            )
            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
                val typeIdx = it.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
                while (it.moveToNext()) {
                    val name = if (nameIdx >= 0) it.getString(nameIdx) else null
                    val type = if (typeIdx >= 0) it.getString(typeIdx) else null
                    if (!name.isNullOrBlank()) {
                        accounts.add(ContactAccount(name = name, type = type, isDevice = (type == null)))
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Always include Phone storage as an option
        accounts.add(ContactAccount(name = "Phone storage", type = null, isDevice = true))

        accounts.toList().sortedWith(compareBy<ContactAccount> {
            when {
                it.type == "com.google" -> 0
                !it.isDevice -> 1
                else -> 2
            }
        })
    }

    suspend fun createContactInSystem(
        displayName: String,
        company: String? = null,
        numbers: List<Pair<String, String>>, // (Number, Label)
        emails: List<String> = emptyList(),
        notes: String? = null,
        accountName: String? = null,
        accountType: String? = null,
        photoBytes: ByteArray? = null
    ): Long? = withContext(Dispatchers.IO) {
        var rawContactId: Long? = null
        try {
            val ops = ArrayList<android.content.ContentProviderOperation>()
            val rawContactInsertIndex = ops.size

            ops.add(
                android.content.ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
                    .build()
            )

            // Name
            val nameParts = displayName.trim().split("\\s+".toRegex(), limit = 2)
            val givenName = nameParts.firstOrNull() ?: ""
            val familyName = if (nameParts.size > 1) nameParts[1] else null
            ops.add(
                android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, givenName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, familyName)
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

            // Emails
            for (email in emails) {
                if (email.isNotBlank()) {
                    ops.add(
                        android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                            .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                            .build()
                    )
                }
            }

            // Notes
            if (!notes.isNullOrBlank()) {
                ops.add(
                    android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, notes)
                        .build()
                )
            }

            // Photo if provided
            if (photoBytes != null && photoBytes.isNotEmpty()) {
                ops.add(
                    android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                        .build()
                )
            }

            val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            if (results.isNotEmpty() && results[0].uri != null) {
                rawContactId = android.content.ContentUris.parseId(results[0].uri!!)
                try {
                    val cur = context.contentResolver.query(
                        ContactsContract.RawContacts.CONTENT_URI,
                        arrayOf(ContactsContract.RawContacts.CONTACT_ID),
                        "${ContactsContract.RawContacts._ID} = ?",
                        arrayOf(rawContactId.toString()),
                        null
                    )
                    cur?.use {
                        if (it.moveToFirst()) {
                            val aggId = it.getLong(0)
                            if (aggId > 0) {
                                rawContactId = aggId
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        rawContactId
    }

    suspend fun createPrivateContact(
        displayName: String,
        company: String? = null,
        numbers: List<Pair<String, String>>, // (Number, Label)
        emails: List<String> = emptyList(),
        preferredSimSlot: Int? = null,
        notes: String? = null,
        photoUri: String? = null
    ): Long = withContext(Dispatchers.IO) {
        // Strictly save locally in Room: androidContactId = null, isPrivate = true
        val contactEntity = ContactEntity(
            androidContactId = null,
            displayName = displayName,
            company = company,
            preferredSimSlot = preferredSimSlot,
            notes = notes,
            photoUri = photoUri,
            isPrivate = true
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

        contactId
    }

    suspend fun deleteContactFromSystemOnly(androidContactId: Long) = withContext(Dispatchers.IO) {
        try {
            val rawUri = android.content.ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, androidContactId)
            context.contentResolver.delete(rawUri, null, null)

            context.contentResolver.delete(
                ContactsContract.RawContacts.CONTENT_URI,
                "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                arrayOf(androidContactId.toString())
            )

            val contactUri = android.content.ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, androidContactId)
            context.contentResolver.delete(contactUri, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        loadContacts()
    }

    suspend fun createContact(
        displayName: String,
        company: String? = null,
        numbers: List<Pair<String, String>>, // (Number, Label)
        emails: List<String> = emptyList(),
        preferredSimSlot: Int? = null,
        notes: String? = null,
        accountName: String? = null,
        accountType: String? = null,
        photoUri: String? = null,
        photoBytes: ByteArray? = null
    ): Long = withContext(Dispatchers.IO) {
        val rawContactId = createContactInSystem(
            displayName = displayName,
            company = company,
            numbers = numbers,
            emails = emails,
            notes = notes,
            accountName = accountName,
            accountType = accountType,
            photoBytes = photoBytes
        )

        // 2. Insert into LINEA local Room database
        val contactEntity = ContactEntity(
            androidContactId = rawContactId,
            displayName = displayName,
            company = company,
            preferredSimSlot = preferredSimSlot,
            notes = notes,
            photoUri = photoUri,
            isPrivate = false
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

    suspend fun updateContact(
        contact: ContactEntity,
        numbers: List<Pair<String, String>>? = null,
        emails: List<String>? = null,
        photoBytes: ByteArray? = null,
        hasPhotoChanged: Boolean = false
    ): ContactEntity = withContext(Dispatchers.IO) {
        var updatedContact = contact

        // 1. Update in Android Contacts Provider if linked or can be linked
        val hasWritePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasWritePermission && !contact.isPrivate) {
            val contactIdOrRawId = contact.androidContactId
            if (contactIdOrRawId != null && contactIdOrRawId > 0) {
                val success = updateContactInSystem(
                    contactIdOrRawId = contactIdOrRawId,
                    displayName = contact.displayName,
                    company = contact.company,
                    numbers = numbers,
                    emails = emails,
                    notes = contact.notes,
                    photoBytes = photoBytes,
                    hasPhotoChanged = hasPhotoChanged
                )
                if (!success) {
                    Log.w("ContactSyncRepo", "updateContactInSystem failed for id $contactIdOrRawId")
                }
            } else {
                // Not yet linked in system contacts: create and link
                val rawId = createContactInSystem(
                    displayName = contact.displayName,
                    company = contact.company,
                    numbers = numbers ?: emptyList(),
                    emails = emails ?: emptyList(),
                    notes = contact.notes,
                    photoBytes = photoBytes
                )
                if (rawId != null && rawId > 0) {
                    updatedContact = updatedContact.copy(androidContactId = rawId)
                }
            }
        }

        // 2. Persist to Room database
        contactDao.updateContact(updatedContact)

        if (numbers != null) {
            val validNumbers = numbers.filter { it.first.isNotBlank() }
            contactDao.deleteNumbersForContact(contact.id)
            if (validNumbers.isNotEmpty()) {
                val numberEntities = validNumbers.mapIndexed { idx, (num, label) ->
                    ContactNumberEntity(
                        contactId = contact.id,
                        number = num,
                        normalizedNumber = num.filter { it.isDigit() || it == '+' },
                        label = label,
                        isPrimary = idx == 0
                    )
                }
                contactDao.insertNumbers(numberEntities)
            }
        }

        if (emails != null) {
            val validEmails = emails.filter { it.isNotBlank() }
            contactDao.deleteEmailsForContact(contact.id)
            if (validEmails.isNotEmpty()) {
                val emailEntities = validEmails.map { email ->
                    ContactEmailEntity(
                        contactId = contact.id,
                        email = email,
                        label = "Home"
                    )
                }
                contactDao.insertEmails(emailEntities)
            }
        }

        // 3. Reload from persistent source and verify
        val reloaded = contactDao.getContactById(contact.id) ?: updatedContact

        // 4. Refresh cached T9 contacts
        loadContacts()

        reloaded
    }

    suspend fun updateContactInSystem(
        contactIdOrRawId: Long,
        displayName: String,
        company: String?,
        numbers: List<Pair<String, String>>?,
        emails: List<String>?,
        notes: String?,
        photoBytes: ByteArray?,
        hasPhotoChanged: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val hasWritePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasWritePermission) {
            Log.w("ContactSyncRepo", "Cannot update contact in system: WRITE_CONTACTS permission not granted")
            return@withContext false
        }

        try {
            // Find all raw contact IDs associated with this Contact ID or RawContact ID
            val rawContactIds = mutableListOf<Long>()
            val rawCursor = context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts._ID),
                "${ContactsContract.RawContacts.CONTACT_ID} = ? OR ${ContactsContract.RawContacts._ID} = ?",
                arrayOf(contactIdOrRawId.toString(), contactIdOrRawId.toString()),
                null
            )
            rawCursor?.use {
                val idIdx = it.getColumnIndex(ContactsContract.RawContacts._ID)
                while (it.moveToNext()) {
                    if (idIdx >= 0) rawContactIds.add(it.getLong(idIdx))
                }
            }
            if (rawContactIds.isEmpty()) {
                rawContactIds.add(contactIdOrRawId)
            }

            val ops = ArrayList<ContentProviderOperation>()

            for (rawId in rawContactIds) {
                // A. StructuredName (DISPLAY_NAME, GIVEN_NAME & FAMILY_NAME)
                val nameParts = displayName.trim().split("\\s+".toRegex(), limit = 2)
                val givenName = nameParts.firstOrNull() ?: ""
                val familyName = if (nameParts.size > 1) nameParts[1] else null

                val nameCursor = context.contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.Data._ID),
                    "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
                    null
                )
                val existingNameId = nameCursor?.use { if (it.moveToFirst()) it.getLong(0) else null }
                if (existingNameId != null) {
                    ops.add(
                        ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(existingNameId.toString()))
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, givenName)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, familyName)
                            .build()
                    )
                } else {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, givenName)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, familyName)
                            .build()
                    )
                }

                // B. Organization (Company)
                val orgCursor = context.contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.Data._ID),
                    "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
                    null
                )
                val existingOrgId = orgCursor?.use { if (it.moveToFirst()) it.getLong(0) else null }
                if (!company.isNullOrBlank()) {
                    if (existingOrgId != null) {
                        ops.add(
                            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                                .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(existingOrgId.toString()))
                                .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
                                .build()
                        )
                    } else {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
                                .build()
                        )
                    }
                } else if (existingOrgId != null) {
                    ops.add(
                        ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                            .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(existingOrgId.toString()))
                            .build()
                    )
                }

                // C. Note
                val noteCursor = context.contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.Data._ID),
                    "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE),
                    null
                )
                val existingNoteId = noteCursor?.use { if (it.moveToFirst()) it.getLong(0) else null }
                if (!notes.isNullOrBlank()) {
                    if (existingNoteId != null) {
                        ops.add(
                            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                                .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(existingNoteId.toString()))
                                .withValue(ContactsContract.CommonDataKinds.Note.NOTE, notes)
                                .build()
                        )
                    } else {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Note.NOTE, notes)
                                .build()
                        )
                    }
                } else if (existingNoteId != null) {
                    ops.add(
                        ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                            .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(existingNoteId.toString()))
                            .build()
                    )
                }

                // D. Phone numbers (if provided)
                if (numbers != null) {
                    ops.add(
                        ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                            .withSelection(
                                "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                                arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            )
                            .build()
                    )
                    for ((num, label) in numbers) {
                        if (num.isNotBlank()) {
                            val phoneType = when (label.lowercase()) {
                                "work" -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
                                "home" -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
                                else -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                            }
                            ops.add(
                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, num)
                                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType)
                                    .build()
                            )
                        }
                    }
                }

                // E. Email addresses (if provided)
                if (emails != null) {
                    ops.add(
                        ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                            .withSelection(
                                "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                                arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            )
                            .build()
                    )
                    for (email in emails) {
                        if (email.isNotBlank()) {
                            ops.add(
                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                                    .build()
                            )
                        }
                    }
                }

                // F. Photo (if changed)
                if (hasPhotoChanged) {
                    val photoCursor = context.contentResolver.query(
                        ContactsContract.Data.CONTENT_URI,
                        arrayOf(ContactsContract.Data._ID),
                        "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE),
                        null
                    )
                    val existingPhotoId = photoCursor?.use { if (it.moveToFirst()) it.getLong(0) else null }
                    if (photoBytes != null && photoBytes.isNotEmpty()) {
                        if (existingPhotoId != null) {
                            ops.add(
                                ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                                    .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(existingPhotoId.toString()))
                                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                                    .build()
                            )
                        } else {
                            ops.add(
                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                                    .build()
                            )
                        }
                    } else if (existingPhotoId != null) {
                        ops.add(
                            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                                .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(existingPhotoId.toString()))
                                .build()
                        )
                    }
                }
            }

            if (ops.isNotEmpty()) {
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            }
            true
        } catch (e: Exception) {
            Log.e("ContactSyncRepo", "Error updating contact in system: ${e.message}", e)
            false
        }
    }

    suspend fun deleteContact(id: Long, androidContactId: Long? = null) = withContext(Dispatchers.IO) {
        contactDao.deleteNumbersForContact(id)
        contactDao.deleteEmailsForContact(id)
        contactDao.removeContactFromAllGroups(id)
        contactDao.deleteContactById(id)
        if (androidContactId != null && androidContactId > 0) {
            try {
                // 1. Delete by RawContacts URI directly (in case androidContactId is raw contact ID)
                val rawUri = android.content.ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, androidContactId)
                context.contentResolver.delete(rawUri, null, null)

                // 2. Delete any raw contacts where CONTACT_ID = androidContactId
                context.contentResolver.delete(
                    ContactsContract.RawContacts.CONTENT_URI,
                    "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                    arrayOf(androidContactId.toString())
                )

                // 3. Delete by Contacts URI (in case androidContactId is aggregate Contact ID)
                val contactUri = android.content.ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, androidContactId)
                context.contentResolver.delete(contactUri, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        loadContacts()
    }
}
