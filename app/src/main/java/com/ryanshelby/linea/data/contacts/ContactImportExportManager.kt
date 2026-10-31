package com.ryanshelby.linea.data.contacts

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.entities.ContactEmailEntity
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.local.entities.ContactNumberEntity
import com.ryanshelby.linea.data.repository.ContactSyncRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

enum class ContactExportFormat(val displayName: String, val extension: String, val mimeType: String) {
    VCF("vCard (.vcf)", "vcf", "text/x-vcard"),
    CSV("Spreadsheet CSV (.csv)", "csv", "text/csv"),
    JSON("Structured JSON (.json)", "json", "application/json")
}

enum class DuplicatePolicy(val displayName: String, val description: String) {
    MERGE_EXISTING("Merge with Existing", "Combine numbers and emails into matching contacts"),
    SKIP_DUPLICATES("Skip Duplicates", "Ignore contacts that already exist in address book"),
    KEEP_BOTH("Keep Both", "Import as new separate contact records")
}

enum class ExportScope(val displayName: String) {
    ALL("All Contacts"),
    FAVORITES_ONLY("Favorites Only")
}

data class ContactExportModel(
    val id: Long? = null,
    val displayName: String,
    val company: String? = null,
    val phoneNumbers: List<Pair<String, String>> = emptyList(), // (Number, Label)
    val emails: List<String> = emptyList(),
    val notes: String? = null,
    val isFavorite: Boolean = false,
    val photoUri: String? = null,
    val accountName: String? = null,
    val accountType: String? = null
)

data class ContactImportSummary(
    val totalRead: Int = 0,
    val importedCount: Int = 0,
    val mergedCount: Int = 0,
    val skippedCount: Int = 0,
    val errorCount: Int = 0,
    val errorMessages: List<String> = emptyList()
)

@Singleton
class ContactImportExportManager @Inject constructor(
    @param:ApplicationContext private val context: Context?,
    private val contactDao: ContactDao?,
    private val contactSyncRepository: ContactSyncRepository?
) {
    constructor() : this(null, null, null)

    /**
     * Retrieves all exportable public contacts.
     * Hard Sovereign Privacy Rule: Vault contacts (isPrivate = 1) are NEVER included!
     */
    suspend fun getExportableContacts(scope: ExportScope = ExportScope.ALL): List<ContactExportModel> = withContext(Dispatchers.IO) {
        val dao = contactDao ?: return@withContext emptyList()
        val publicContacts = dao.getAllPublicContactsOnce()
        val filtered = when (scope) {
            ExportScope.ALL -> publicContacts
            ExportScope.FAVORITES_ONLY -> publicContacts.filter { it.isFavorite }
        }

        filtered.map { contact ->
            val numbers = dao.getNumbersForContactOnce(contact.id).map { it.number to (it.label ?: "Mobile") }
            val emails = dao.getEmailsForContactOnce(contact.id).map { it.email }
            ContactExportModel(
                id = contact.id,
                displayName = contact.displayName,
                company = contact.company,
                phoneNumbers = numbers,
                emails = emails,
                notes = contact.notes,
                isFavorite = contact.isFavorite,
                photoUri = contact.photoUri
            )
        }
    }

    /**
     * Serializes contact models to standard vCard 3.0 format.
     */
    fun generateVcfString(contacts: List<ContactExportModel>): String {
        val sb = StringBuilder()
        for (c in contacts) {
            sb.append("BEGIN:VCARD\r\n")
            sb.append("VERSION:3.0\r\n")
            sb.append("FN:${escapeVcf(c.displayName)}\r\n")

            val nameParts = c.displayName.trim().split("\\s+".toRegex(), limit = 2)
            val given = nameParts.firstOrNull() ?: ""
            val family = if (nameParts.size > 1) nameParts[1] else ""
            sb.append("N:${escapeVcf(family)};${escapeVcf(given)};;;\r\n")

            if (!c.company.isNullOrBlank()) {
                sb.append("ORG:${escapeVcf(c.company)}\r\n")
            }

            for ((number, label) in c.phoneNumbers) {
                val type = when (label.uppercase()) {
                    "WORK" -> "WORK"
                    "HOME" -> "HOME"
                    else -> "CELL"
                }
                sb.append("TEL;TYPE=$type:${number.trim()}\r\n")
            }

            for (email in c.emails) {
                if (email.isNotBlank()) {
                    sb.append("EMAIL;TYPE=INTERNET:${email.trim()}\r\n")
                }
            }

            if (!c.notes.isNullOrBlank()) {
                sb.append("NOTE:${escapeVcf(c.notes)}\r\n")
            }

            if (c.isFavorite) {
                sb.append("X-STARRED:1\r\n")
            }

            sb.append("END:VCARD\r\n")
        }
        return sb.toString()
    }

    /**
     * Parses standard vCard (vcf 2.1 / 3.0) content into contact models.
     */
    fun parseVcfString(vcfContent: String): List<ContactExportModel> {
        val contacts = mutableListOf<ContactExportModel>()
        val lines = unfoldLines(vcfContent)

        var inVCard = false
        var displayName = ""
        var givenName = ""
        var familyName = ""
        var company: String? = null
        val phones = mutableListOf<Pair<String, String>>()
        val emails = mutableListOf<String>()
        var notes: String? = null
        var isFavorite = false

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.equals("BEGIN:VCARD", ignoreCase = true)) {
                inVCard = true
                displayName = ""
                givenName = ""
                familyName = ""
                company = null
                phones.clear()
                emails.clear()
                notes = null
                isFavorite = false
                continue
            }

            if (line.equals("END:VCARD", ignoreCase = true)) {
                if (inVCard) {
                    val finalName = when {
                        displayName.isNotBlank() -> displayName
                        givenName.isNotBlank() || familyName.isNotBlank() -> "$givenName $familyName".trim()
                        phones.isNotEmpty() -> phones.first().first
                        else -> "Unnamed Contact"
                    }
                    contacts.add(
                        ContactExportModel(
                            displayName = finalName,
                            company = company,
                            phoneNumbers = phones.toList(),
                            emails = emails.toList(),
                            notes = notes,
                            isFavorite = isFavorite
                        )
                    )
                    inVCard = false
                }
                continue
            }

            if (!inVCard) continue

            val colonIdx = line.indexOf(':')
            if (colonIdx <= 0) continue

            val tagAndParams = line.substring(0, colonIdx)
            val value = unescapeVcf(line.substring(colonIdx + 1))
            val tagUpper = tagAndParams.uppercase()

            when {
                tagUpper.startsWith("FN") -> {
                    displayName = value
                }
                tagUpper.startsWith("N") && !tagUpper.startsWith("NOTE") -> {
                    val parts = value.split(';')
                    familyName = parts.getOrNull(0) ?: ""
                    givenName = parts.getOrNull(1) ?: ""
                }
                tagUpper.startsWith("ORG") -> {
                    company = value
                }
                tagUpper.startsWith("TEL") -> {
                    val label = when {
                        tagUpper.contains("WORK") -> "Work"
                        tagUpper.contains("HOME") -> "Home"
                        else -> "Mobile"
                    }
                    if (value.isNotBlank()) {
                        phones.add(value to label)
                    }
                }
                tagUpper.startsWith("EMAIL") -> {
                    if (value.isNotBlank()) {
                        emails.add(value)
                    }
                }
                tagUpper.startsWith("NOTE") -> {
                    notes = value
                }
                tagUpper.startsWith("X-STARRED") -> {
                    isFavorite = value.trim() == "1" || value.trim().equals("true", ignoreCase = true)
                }
            }
        }

        return contacts
    }

    /**
     * Serializes contact models to CSV format conforming to RFC 4180.
     */
    fun generateCsvString(contacts: List<ContactExportModel>): String {
        val sb = StringBuilder()
        sb.append("Display Name,Company,Phone 1,Phone 1 Label,Phone 2,Phone 2 Label,Phone 3,Phone 3 Label,Email 1,Email 2,Notes,Favorite\r\n")

        for (c in contacts) {
            val p1 = c.phoneNumbers.getOrNull(0)
            val p2 = c.phoneNumbers.getOrNull(1)
            val p3 = c.phoneNumbers.getOrNull(2)
            val e1 = c.emails.getOrNull(0) ?: ""
            val e2 = c.emails.getOrNull(1) ?: ""

            sb.append(escapeCsv(c.displayName)).append(",")
            sb.append(escapeCsv(c.company ?: "")).append(",")
            sb.append(escapeCsv(p1?.first ?: "")).append(",")
            sb.append(escapeCsv(p1?.second ?: "")).append(",")
            sb.append(escapeCsv(p2?.first ?: "")).append(",")
            sb.append(escapeCsv(p2?.second ?: "")).append(",")
            sb.append(escapeCsv(p3?.first ?: "")).append(",")
            sb.append(escapeCsv(p3?.second ?: "")).append(",")
            sb.append(escapeCsv(e1)).append(",")
            sb.append(escapeCsv(e2)).append(",")
            sb.append(escapeCsv(c.notes ?: "")).append(",")
            sb.append(if (c.isFavorite) "1" else "0").append("\r\n")
        }

        return sb.toString()
    }

    /**
     * Parses standard CSV format into contact models.
     */
    fun parseCsvString(csvContent: String): List<ContactExportModel> {
        val contacts = mutableListOf<ContactExportModel>()
        val rows = parseCsvRows(csvContent)
        if (rows.isEmpty()) return contacts

        val header = rows.first().map { it.trim().lowercase() }
        val nameIdx = header.indexOfFirst { it.contains("name") }
        val compIdx = header.indexOfFirst { it.contains("comp") }
        val p1Idx = header.indexOfFirst { it.contains("phone 1") || it == "phone" || it == "number" }
        val p1LblIdx = header.indexOfFirst { it.contains("phone 1 label") || it == "label" }
        val p2Idx = header.indexOfFirst { it.contains("phone 2") }
        val p2LblIdx = header.indexOfFirst { it.contains("phone 2 label") }
        val p3Idx = header.indexOfFirst { it.contains("phone 3") }
        val p3LblIdx = header.indexOfFirst { it.contains("phone 3 label") }
        val e1Idx = header.indexOfFirst { it.contains("email 1") || it == "email" }
        val e2Idx = header.indexOfFirst { it.contains("email 2") }
        val notesIdx = header.indexOfFirst { it.contains("note") }
        val favIdx = header.indexOfFirst { it.contains("fav") || it.contains("star") }

        for (row in rows.drop(1)) {
            val name = row.getOrNull(if (nameIdx >= 0) nameIdx else 0)?.trim() ?: ""
            val comp = if (compIdx >= 0) row.getOrNull(compIdx)?.trim()?.takeIf { it.isNotBlank() } else null
            val phones = mutableListOf<Pair<String, String>>()

            val num1 = if (p1Idx >= 0) row.getOrNull(p1Idx)?.trim() ?: "" else ""
            val lbl1 = if (p1LblIdx >= 0) row.getOrNull(p1LblIdx)?.trim()?.takeIf { it.isNotBlank() } ?: "Mobile" else "Mobile"
            if (num1.isNotBlank()) phones.add(num1 to lbl1)

            val num2 = if (p2Idx >= 0) row.getOrNull(p2Idx)?.trim() ?: "" else ""
            val lbl2 = if (p2LblIdx >= 0) row.getOrNull(p2LblIdx)?.trim()?.takeIf { it.isNotBlank() } ?: "Work" else "Work"
            if (num2.isNotBlank()) phones.add(num2 to lbl2)

            val num3 = if (p3Idx >= 0) row.getOrNull(p3Idx)?.trim() ?: "" else ""
            val lbl3 = if (p3LblIdx >= 0) row.getOrNull(p3LblIdx)?.trim()?.takeIf { it.isNotBlank() } ?: "Home" else "Home"
            if (num3.isNotBlank()) phones.add(num3 to lbl3)

            val emails = mutableListOf<String>()
            val em1 = if (e1Idx >= 0) row.getOrNull(e1Idx)?.trim() ?: "" else ""
            if (em1.isNotBlank()) emails.add(em1)
            val em2 = if (e2Idx >= 0) row.getOrNull(e2Idx)?.trim() ?: "" else ""
            if (em2.isNotBlank()) emails.add(em2)

            val notes = if (notesIdx >= 0) row.getOrNull(notesIdx)?.trim()?.takeIf { it.isNotBlank() } else null
            val fav = if (favIdx >= 0) {
                val fv = row.getOrNull(favIdx)?.trim() ?: ""
                fv == "1" || fv.equals("true", ignoreCase = true) || fv.equals("yes", ignoreCase = true)
            } else false

            if (name.isNotBlank() || phones.isNotEmpty() || emails.isNotEmpty()) {
                contacts.add(
                    ContactExportModel(
                        displayName = if (name.isNotBlank()) name else phones.firstOrNull()?.first ?: "Unnamed Contact",
                        company = comp,
                        phoneNumbers = phones,
                        emails = emails,
                        notes = notes,
                        isFavorite = fav
                    )
                )
            }
        }

        return contacts
    }

    /**
     * Serializes contact models to JSON format.
     */
    fun generateJsonString(contacts: List<ContactExportModel>): String {
        val array = JSONArray()
        for (c in contacts) {
            val obj = JSONObject()
            obj.put("displayName", c.displayName)
            if (c.company != null) obj.put("company", c.company)

            val phonesArr = JSONArray()
            for ((num, label) in c.phoneNumbers) {
                val pObj = JSONObject()
                pObj.put("number", num)
                pObj.put("label", label)
                phonesArr.put(pObj)
            }
            obj.put("phoneNumbers", phonesArr)

            val emailsArr = JSONArray()
            for (em in c.emails) {
                emailsArr.put(em)
            }
            obj.put("emails", emailsArr)

            if (c.notes != null) obj.put("notes", c.notes)
            obj.put("isFavorite", c.isFavorite)
            array.put(obj)
        }
        return array.toString(2)
    }

    /**
     * Parses JSON string into contact models.
     */
    fun parseJsonString(jsonContent: String): List<ContactExportModel> {
        val contacts = mutableListOf<ContactExportModel>()
        val array = JSONArray(jsonContent.trim())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val name = obj.optString("displayName", "")
            val comp = obj.optString("company", "").takeIf { it.isNotBlank() }
            val phones = mutableListOf<Pair<String, String>>()
            val phonesArr = obj.optJSONArray("phoneNumbers")
            if (phonesArr != null) {
                for (j in 0 until phonesArr.length()) {
                    val pObj = phonesArr.getJSONObject(j)
                    val num = pObj.optString("number", "")
                    val lbl = pObj.optString("label", "Mobile")
                    if (num.isNotBlank()) phones.add(num to lbl)
                }
            }

            val emails = mutableListOf<String>()
            val emailsArr = obj.optJSONArray("emails")
            if (emailsArr != null) {
                for (j in 0 until emailsArr.length()) {
                    val em = emailsArr.optString(j, "")
                    if (em.isNotBlank()) emails.add(em)
                }
            }

            val notes = obj.optString("notes", "").takeIf { it.isNotBlank() }
            val fav = obj.optBoolean("isFavorite", false)

            if (name.isNotBlank() || phones.isNotEmpty() || emails.isNotEmpty()) {
                contacts.add(
                    ContactExportModel(
                        displayName = if (name.isNotBlank()) name else phones.firstOrNull()?.first ?: "Unnamed Contact",
                        company = comp,
                        phoneNumbers = phones,
                        emails = emails,
                        notes = notes,
                        isFavorite = fav
                    )
                )
            }
        }
        return contacts
    }

    /**
     * Exports contacts directly to a Storage Access Framework Uri.
     */
    suspend fun exportContactsToUri(
        uri: Uri,
        format: ContactExportFormat,
        contacts: List<ContactExportModel>,
        contentResolver: ContentResolver
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val content = when (format) {
                ContactExportFormat.VCF -> generateVcfString(contacts)
                ContactExportFormat.CSV -> generateCsvString(contacts)
                ContactExportFormat.JSON -> generateJsonString(contacts)
            }

            contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os, StandardCharsets.UTF_8).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            } ?: return@withContext Result.failure(Exception("Cannot open destination stream"))

            Result.success(contacts.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads contacts from a Storage Access Framework Uri.
     */
    suspend fun readContactsFromUri(
        uri: Uri,
        format: ContactExportFormat?,
        contentResolver: ContentResolver
    ): Result<List<ContactExportModel>> = withContext(Dispatchers.IO) {
        try {
            val stringBuilder = StringBuilder()
            contentResolver.openInputStream(uri)?.use { isStream ->
                BufferedReader(InputStreamReader(isStream, StandardCharsets.UTF_8)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            } ?: return@withContext Result.failure(Exception("Cannot open source stream"))

            val content = stringBuilder.toString()
            val detectedFormat = format ?: detectFormatFromContentOrUri(content, uri.toString())

            val list = when (detectedFormat) {
                ContactExportFormat.VCF -> parseVcfString(content)
                ContactExportFormat.CSV -> parseCsvString(content)
                ContactExportFormat.JSON -> parseJsonString(content)
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Imports contacts into the device / app using the specified DuplicatePolicy.
     */
    suspend fun importContacts(
        contacts: List<ContactExportModel>,
        policy: DuplicatePolicy,
        accountName: String?,
        accountType: String?
    ): ContactImportSummary = withContext(Dispatchers.IO) {
        val dao = contactDao ?: return@withContext ContactImportSummary(errorCount = 1, errorMessages = listOf("ContactDao unavailable"))
        val syncRepo = contactSyncRepository ?: return@withContext ContactImportSummary(errorCount = 1, errorMessages = listOf("ContactSyncRepository unavailable"))

        var imported = 0
        var merged = 0
        var skipped = 0
        var errors = 0
        val errorMsgs = mutableListOf<String>()

        val existingContacts = dao.getAllPublicContactsOnce()
        val allNumbers = mutableMapOf<String, Long>() // normalized -> contactId
        for (c in existingContacts) {
            val nums = dao.getNumbersForContactOnce(c.id)
            for (n in nums) {
                if (n.normalizedNumber.isNotBlank()) {
                    allNumbers[n.normalizedNumber] = c.id
                }
            }
        }

        for (item in contacts) {
            try {
                // Find matching existing contact
                var matchedContactId: Long? = null
                for ((num, _) in item.phoneNumbers) {
                    val norm = num.filter { it.isDigit() || it == '+' }
                    if (norm.length >= 6 && allNumbers.containsKey(norm)) {
                        matchedContactId = allNumbers[norm]
                        break
                    }
                }

                if (matchedContactId == null) {
                    val nameMatch = existingContacts.firstOrNull {
                        it.displayName.equals(item.displayName.trim(), ignoreCase = true)
                    }
                    if (nameMatch != null) {
                        matchedContactId = nameMatch.id
                    }
                }

                if (matchedContactId != null) {
                    when (policy) {
                        DuplicatePolicy.SKIP_DUPLICATES -> {
                            skipped++
                            continue
                        }
                        DuplicatePolicy.MERGE_EXISTING -> {
                            val existingContact = dao.getContactById(matchedContactId)
                            if (existingContact != null && !existingContact.isPrivate) {
                                // Union phone numbers
                                val currentNumbers = dao.getNumbersForContactOnce(matchedContactId)
                                val currentNormSet = currentNumbers.map { it.normalizedNumber }.toSet()

                                val newNumberEntities = mutableListOf<ContactNumberEntity>()
                                for ((num, label) in item.phoneNumbers) {
                                    val norm = num.filter { it.isDigit() || it == '+' }
                                    if (norm.isNotBlank() && !currentNormSet.contains(norm)) {
                                        newNumberEntities.add(
                                            ContactNumberEntity(
                                                contactId = matchedContactId,
                                                number = num,
                                                normalizedNumber = norm,
                                                label = label,
                                                isPrimary = false
                                            )
                                        )
                                        allNumbers[norm] = matchedContactId
                                    }
                                }
                                if (newNumberEntities.isNotEmpty()) {
                                    dao.insertNumbers(newNumberEntities)
                                }

                                // Union emails
                                val currentEmails = dao.getEmailsForContactOnce(matchedContactId)
                                val currentEmailSet = currentEmails.map { it.email.lowercase() }.toSet()
                                val newEmailEntities = mutableListOf<ContactEmailEntity>()
                                for (em in item.emails) {
                                    if (em.isNotBlank() && !currentEmailSet.contains(em.lowercase())) {
                                        newEmailEntities.add(
                                            ContactEmailEntity(
                                                contactId = matchedContactId,
                                                email = em,
                                                label = "Work"
                                            )
                                        )
                                    }
                                }
                                if (newEmailEntities.isNotEmpty()) {
                                    dao.insertEmails(newEmailEntities)
                                }

                                // Merge notes
                                val updatedNotes = when {
                                    existingContact.notes.isNullOrBlank() -> item.notes
                                    item.notes.isNullOrBlank() -> existingContact.notes
                                    existingContact.notes.contains(item.notes) -> existingContact.notes
                                    else -> "${existingContact.notes}\n${item.notes}"
                                }

                                dao.updateContact(
                                    existingContact.copy(
                                        notes = updatedNotes,
                                        isFavorite = existingContact.isFavorite || item.isFavorite
                                    )
                                )
                                merged++
                                continue
                            }
                        }
                        DuplicatePolicy.KEEP_BOTH -> {
                            // Proceed to insert as new contact
                        }
                    }
                }

                // Insert as new contact
                val newId = syncRepo.createContact(
                    displayName = item.displayName,
                    company = item.company,
                    numbers = item.phoneNumbers,
                    emails = item.emails,
                    notes = item.notes,
                    accountName = accountName,
                    accountType = accountType
                )

                // Cache numbers
                for ((num, _) in item.phoneNumbers) {
                    val norm = num.filter { it.isDigit() || it == '+' }
                    if (norm.isNotBlank()) {
                        allNumbers[norm] = newId
                    }
                }
                imported++

            } catch (e: Exception) {
                errors++
                errorMsgs.add("${item.displayName}: ${e.message}")
            }
        }

        // Trigger local contacts reload
        syncRepo.loadContacts()

        ContactImportSummary(
            totalRead = contacts.size,
            importedCount = imported,
            mergedCount = merged,
            skippedCount = skipped,
            errorCount = errors,
            errorMessages = errorMsgs
        )
    }

    // Helper functions

    private fun detectFormatFromContentOrUri(content: String, uriStr: String): ContactExportFormat {
        val lowerUri = uriStr.lowercase()
        return when {
            lowerUri.endsWith(".vcf") || content.contains("BEGIN:VCARD", ignoreCase = true) -> ContactExportFormat.VCF
            lowerUri.endsWith(".csv") || content.startsWith("Display Name,") -> ContactExportFormat.CSV
            lowerUri.endsWith(".json") || content.trimStart().startsWith("[") || content.trimStart().startsWith("{") -> ContactExportFormat.JSON
            else -> ContactExportFormat.VCF
        }
    }

    private fun escapeVcf(value: String): String {
        return value.replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\n", "\\n")
    }

    private fun unescapeVcf(value: String): String {
        return value.replace("\\n", "\n")
            .replace("\\;", ";")
            .replace("\\,", ",")
            .replace("\\\\", "\\")
    }

    private fun unfoldLines(content: String): List<String> {
        val result = mutableListOf<String>()
        val rawLines = content.lines()
        var current: StringBuilder? = null

        for (line in rawLines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                current?.append(line.substring(1))
            } else {
                if (current != null) {
                    result.add(current.toString())
                }
                current = StringBuilder(line)
            }
        }
        if (current != null) {
            result.add(current.toString())
        }
        return result
    }

    private fun escapeCsv(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
    }

    private fun parseCsvRows(csv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var insideQuotes = false
        var i = 0

        while (i < csv.length) {
            val c = csv[i]
            if (insideQuotes) {
                if (c == '"') {
                    if (i + 1 < csv.length && csv[i + 1] == '"') {
                        currentField.append('"')
                        i++ // Skip escaped quote
                    } else {
                        insideQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> insideQuotes = true
                    ',' -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                    }
                    '\r' -> {
                        if (i + 1 < csv.length && csv[i + 1] == '\n') i++
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        rows.add(currentRow)
                        currentRow = mutableListOf()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        rows.add(currentRow)
                        currentRow = mutableListOf()
                    }
                    else -> currentField.append(c)
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            rows.add(currentRow)
        }

        return rows
    }
}
