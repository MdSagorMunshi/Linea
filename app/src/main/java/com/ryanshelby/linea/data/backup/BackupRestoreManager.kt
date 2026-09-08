package com.ryanshelby.linea.data.backup

import com.ryanshelby.linea.data.local.dao.CallRecordDao
import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.entities.CallDirectionType
import com.ryanshelby.linea.data.local.entities.CallRecordEntity
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.local.entities.ContactNumberEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class BackupRestoreResult(
    val success: Boolean,
    val restoredContacts: Int = 0,
    val restoredCalls: Int = 0,
    val message: String
)

@Singleton
class BackupRestoreManager @Inject constructor(
    private val callRecordDao: CallRecordDao,
    private val contactDao: ContactDao
) {

    suspend fun generateBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("app", "LINEA")
        root.put("timestamp", System.currentTimeMillis())

        // 1. Call Records
        val callRecords = callRecordDao.getAllRecordsOnce()
        val callsArray = JSONArray()
        for (record in callRecords) {
            val callObj = JSONObject().apply {
                put("phoneNumber", record.phoneNumber)
                put("formattedNumber", record.formattedNumber)
                put("callerName", record.callerName ?: "")
                put("callType", record.callType.name)
                put("timestamp", record.timestamp)
                put("durationSeconds", record.durationSeconds)
                put("simSlot", record.simSlot)
                put("simDisplayName", record.simDisplayName ?: "")
                put("networkType", record.networkType)
                put("isPrivateContact", record.isPrivateContact)
                put("notes", record.notes ?: "")
            }
            callsArray.put(callObj)
        }
        root.put("callRecords", callsArray)

        // 2. Contacts & Numbers
        val contacts = contactDao.getAllContactsIncludingPrivate().first()
        val contactsArray = JSONArray()
        for (contact in contacts) {
            val contactObj = JSONObject().apply {
                put("displayName", contact.displayName)
                put("isFavorite", contact.isFavorite)
                put("isPinned", contact.isPinned)
                put("isPrivate", contact.isPrivate)
                put("company", contact.company ?: "")
                put("jobTitle", contact.jobTitle ?: "")
                put("preferredSimSlot", contact.preferredSimSlot ?: -1)
                put("allowDuringRestrictedHours", contact.allowDuringRestrictedHours)
                put("alwaysRing", contact.alwaysRing)
                put("notes", contact.notes ?: "")

                val numbers = contactDao.getNumbersForContact(contact.id).first()
                val numbersArray = JSONArray()
                for (number in numbers) {
                    val numObj = JSONObject().apply {
                        put("number", number.number)
                        put("normalizedNumber", number.normalizedNumber)
                        put("label", number.label)
                        put("isPrimary", number.isPrimary)
                    }
                    numbersArray.put(numObj)
                }
                put("numbers", numbersArray)
            }
            contactsArray.put(contactObj)
        }
        root.put("contacts", contactsArray)

        root.toString(2)
    }

    suspend fun restoreFromJson(jsonString: String): BackupRestoreResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("app") || root.getString("app") != "LINEA") {
                return@withContext BackupRestoreResult(
                    success = false,
                    message = "Invalid backup format: missing LINEA signature."
                )
            }

            var restoredCalls = 0
            var restoredContacts = 0

            // Restore Contacts
            if (root.has("contacts")) {
                val contactsArray = root.getJSONArray("contacts")
                for (i in 0 until contactsArray.length()) {
                    val cObj = contactsArray.getJSONObject(i)
                    val preferredSim = cObj.optInt("preferredSimSlot", -1)
                    val contact = ContactEntity(
                        displayName = cObj.getString("displayName"),
                        isFavorite = cObj.optBoolean("isFavorite", false),
                        isPinned = cObj.optBoolean("isPinned", false),
                        isPrivate = cObj.optBoolean("isPrivate", false),
                        company = cObj.optString("company").takeIf { it.isNotEmpty() },
                        jobTitle = cObj.optString("jobTitle").takeIf { it.isNotEmpty() },
                        preferredSimSlot = if (preferredSim >= 0) preferredSim else null,
                        allowDuringRestrictedHours = cObj.optBoolean("allowDuringRestrictedHours", false),
                        alwaysRing = cObj.optBoolean("alwaysRing", false),
                        notes = cObj.optString("notes").takeIf { it.isNotEmpty() }
                    )
                    val contactId = contactDao.insertContact(contact)

                    if (cObj.has("numbers")) {
                        val numsArray = cObj.getJSONArray("numbers")
                        val numbersList = mutableListOf<ContactNumberEntity>()
                        for (j in 0 until numsArray.length()) {
                            val nObj = numsArray.getJSONObject(j)
                            numbersList.add(
                                ContactNumberEntity(
                                    contactId = contactId,
                                    number = nObj.getString("number"),
                                    normalizedNumber = nObj.getString("normalizedNumber"),
                                    label = nObj.optString("label", "Mobile"),
                                    isPrimary = nObj.optBoolean("isPrimary", j == 0)
                                )
                            )
                        }
                        contactDao.insertNumbers(numbersList)
                    }
                    restoredContacts++
                }
            }

            // Restore Call Records
            if (root.has("callRecords")) {
                val callsArray = root.getJSONArray("callRecords")
                for (i in 0 until callsArray.length()) {
                    val callObj = callsArray.getJSONObject(i)
                    val direction = try {
                        CallDirectionType.valueOf(callObj.getString("callType"))
                    } catch (e: Exception) {
                        CallDirectionType.INCOMING
                    }
                    val record = CallRecordEntity(
                        phoneNumber = callObj.getString("phoneNumber"),
                        formattedNumber = callObj.optString("formattedNumber", callObj.getString("phoneNumber")),
                        callerName = callObj.optString("callerName").takeIf { it.isNotEmpty() },
                        callType = direction,
                        timestamp = callObj.getLong("timestamp"),
                        durationSeconds = callObj.optLong("durationSeconds", 0),
                        simSlot = callObj.optInt("simSlot", 0),
                        simDisplayName = callObj.optString("simDisplayName").takeIf { it.isNotEmpty() },
                        networkType = callObj.optString("networkType", "LTE"),
                        isPrivateContact = callObj.optBoolean("isPrivateContact", false),
                        notes = callObj.optString("notes").takeIf { it.isNotEmpty() }
                    )
                    callRecordDao.insertCallRecord(record)
                    restoredCalls++
                }
            }

            BackupRestoreResult(
                success = true,
                restoredContacts = restoredContacts,
                restoredCalls = restoredCalls,
                message = "Restored $restoredContacts contacts and $restoredCalls call records successfully."
            )
        } catch (e: Exception) {
            BackupRestoreResult(
                success = false,
                message = "Failed to restore backup: ${e.localizedMessage ?: "Unknown parse error"}"
            )
        }
    }
}
