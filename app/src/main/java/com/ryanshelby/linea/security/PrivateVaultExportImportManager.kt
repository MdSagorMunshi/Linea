package com.ryanshelby.linea.security

import com.ryanshelby.linea.data.local.dao.ContactDao
import com.ryanshelby.linea.data.local.entities.ContactEmailEntity
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.data.local.entities.ContactNumberEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

data class PrivateContactExportDto(
    val displayName: String,
    val company: String? = null,
    val jobTitle: String? = null,
    val nickname: String? = null,
    val notes: String? = null,
    val numbers: List<PrivateNumberExportDto> = emptyList(),
    val emails: List<PrivateEmailExportDto> = emptyList()
)

data class PrivateNumberExportDto(
    val number: String,
    val label: String = "Mobile",
    val isPrimary: Boolean = false
)

data class PrivateEmailExportDto(
    val email: String,
    val label: String = "Work"
)

@Singleton
class PrivateVaultExportImportManager @Inject constructor(
    private val contactDao: ContactDao
) {
    companion object {
        // 12-byte unique magic header for .linea vault files: LINEA_VAULT\u0001
        val MAGIC_HEADER = "LINEA_VAULT\u0001".toByteArray(StandardCharsets.UTF_8)
        private const val SALT_LENGTH_BYTES = 32
        private const val IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val PBKDF2_ITERATIONS = 100_000
        private const val AES_KEY_SIZE_BITS = 256
        private const val MIN_FILE_SIZE = 12 + SALT_LENGTH_BYTES + IV_LENGTH_BYTES + 16 // 72 bytes
    }

    private val secureRandom = SecureRandom()

    /**
     * Gathers all private contacts and their phone numbers and emails from the database.
     */
    suspend fun getPrivateContactsForExport(): List<PrivateContactExportDto> = withContext(Dispatchers.IO) {
        val contacts = contactDao.getPrivateContacts().first()
        contacts.map { contact ->
            val numbers = contactDao.getNumbersForContact(contact.id).first()
            val emails = contactDao.getEmailsForContact(contact.id).first()
            PrivateContactExportDto(
                displayName = contact.displayName,
                company = contact.company,
                jobTitle = contact.jobTitle,
                nickname = contact.nickname,
                notes = contact.notes,
                numbers = numbers.map {
                    PrivateNumberExportDto(
                        number = it.number,
                        label = it.label,
                        isPrimary = it.isPrimary
                    )
                },
                emails = emails.map {
                    PrivateEmailExportDto(
                        email = it.email,
                        label = it.label
                    )
                }
            )
        }
    }

    /**
     * Encrypts private contact list into a post-quantum .linea binary format using PBKDF2-HMAC-SHA512 + AES-256-GCM.
     */
    fun exportToEncryptedLineaBytes(
        contacts: List<PrivateContactExportDto>,
        encryptionKey: String
    ): ByteArray {
        require(encryptionKey.isNotBlank()) { "Encryption key cannot be empty" }

        // 1. Serialize contacts to JSON
        val root = JSONObject().apply {
            put("format", "LINEA_PRIVATE_VAULT")
            put("version", 1)
            put("timestamp", System.currentTimeMillis())
            put("count", contacts.size)

            val contactsArray = JSONArray()
            for (c in contacts) {
                val cObj = JSONObject().apply {
                    put("displayName", c.displayName)
                    put("company", c.company ?: "")
                    put("jobTitle", c.jobTitle ?: "")
                    put("nickname", c.nickname ?: "")
                    put("notes", c.notes ?: "")

                    val numArr = JSONArray()
                    for (n in c.numbers) {
                        numArr.put(JSONObject().apply {
                            put("number", n.number)
                            put("label", n.label)
                            put("isPrimary", n.isPrimary)
                        })
                    }
                    put("numbers", numArr)

                    val emailArr = JSONArray()
                    for (e in c.emails) {
                        emailArr.put(JSONObject().apply {
                            put("email", e.email)
                            put("label", e.label)
                        })
                    }
                    put("emails", emailArr)
                }
                contactsArray.put(cObj)
            }
            put("contacts", contactsArray)
        }

        val plaintextBytes = root.toString().toByteArray(StandardCharsets.UTF_8)

        // 2. Generate random 32-byte salt and 12-byte IV
        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }

        // 3. Derive 256-bit AES key with PBKDF2WithHmacSHA512
        val derivedKey = deriveAesKey(encryptionKey.toCharArray(), salt)

        // 4. Encrypt with AES-256-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(derivedKey, "AES"), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(plaintextBytes)

        // 5. Construct container: [MAGIC (12)] + [SALT (32)] + [IV (12)] + [CIPHERTEXT + TAG]
        val output = ByteArray(MAGIC_HEADER.size + salt.size + iv.size + ciphertext.size)
        var offset = 0

        System.arraycopy(MAGIC_HEADER, 0, output, offset, MAGIC_HEADER.size)
        offset += MAGIC_HEADER.size

        System.arraycopy(salt, 0, output, offset, salt.size)
        offset += salt.size

        System.arraycopy(iv, 0, output, offset, iv.size)
        offset += iv.size

        System.arraycopy(ciphertext, 0, output, offset, ciphertext.size)

        return output
    }

    /**
     * Decrypts a .linea file using the provided PIN or custom password.
     */
    fun decryptAndParseLineaBytes(
        fileBytes: ByteArray,
        decryptionKey: String
    ): Result<List<PrivateContactExportDto>> {
        if (fileBytes.size < MIN_FILE_SIZE) {
            return Result.failure(IllegalArgumentException("File is too small to be a valid .linea vault container"))
        }

        // 1. Verify Magic Header
        for (i in MAGIC_HEADER.indices) {
            if (fileBytes[i] != MAGIC_HEADER[i]) {
                return Result.failure(IllegalArgumentException("Invalid .linea vault file format. Missing signature."))
            }
        }

        try {
            var offset = MAGIC_HEADER.size

            // 2. Extract salt
            val salt = ByteArray(SALT_LENGTH_BYTES)
            System.arraycopy(fileBytes, offset, salt, 0, SALT_LENGTH_BYTES)
            offset += SALT_LENGTH_BYTES

            // 3. Extract IV
            val iv = ByteArray(IV_LENGTH_BYTES)
            System.arraycopy(fileBytes, offset, iv, 0, IV_LENGTH_BYTES)
            offset += IV_LENGTH_BYTES

            // 4. Extract Ciphertext + Tag
            val ciphertextSize = fileBytes.size - offset
            val ciphertext = ByteArray(ciphertextSize)
            System.arraycopy(fileBytes, offset, ciphertext, 0, ciphertextSize)

            // 5. Derive key
            val derivedKey = deriveAesKey(decryptionKey.toCharArray(), salt)

            // 6. Decrypt
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(derivedKey, "AES"), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val decryptedBytes = cipher.doFinal(ciphertext)

            // 7. Parse JSON
            val jsonString = String(decryptedBytes, StandardCharsets.UTF_8)
            val root = JSONObject(jsonString)
            val contactsArray = root.optJSONArray("contacts") ?: JSONArray()

            val list = mutableListOf<PrivateContactExportDto>()
            for (i in 0 until contactsArray.length()) {
                val cObj = contactsArray.getJSONObject(i)
                val displayName = cObj.optString("displayName", "Unnamed Contact")
                val company = cObj.optString("company").takeIf { it.isNotBlank() }
                val jobTitle = cObj.optString("jobTitle").takeIf { it.isNotBlank() }
                val nickname = cObj.optString("nickname").takeIf { it.isNotBlank() }
                val notes = cObj.optString("notes").takeIf { it.isNotBlank() }

                val numbersList = mutableListOf<PrivateNumberExportDto>()
                val numArr = cObj.optJSONArray("numbers")
                if (numArr != null) {
                    for (j in 0 until numArr.length()) {
                        val nObj = numArr.getJSONObject(j)
                        val num = nObj.optString("number")
                        val label = nObj.optString("label", "Mobile")
                        val isPrimary = nObj.optBoolean("isPrimary", j == 0)
                        if (num.isNotBlank()) {
                            numbersList.add(PrivateNumberExportDto(num, label, isPrimary))
                        }
                    }
                }

                val emailsList = mutableListOf<PrivateEmailExportDto>()
                val emailArr = cObj.optJSONArray("emails")
                if (emailArr != null) {
                    for (j in 0 until emailArr.length()) {
                        val eObj = emailArr.getJSONObject(j)
                        val email = eObj.optString("email")
                        val label = eObj.optString("label", "Work")
                        if (email.isNotBlank()) {
                            emailsList.add(PrivateEmailExportDto(email, label))
                        }
                    }
                }

                list.add(
                    PrivateContactExportDto(
                        displayName = displayName,
                        company = company,
                        jobTitle = jobTitle,
                        nickname = nickname,
                        notes = notes,
                        numbers = numbersList,
                        emails = emailsList
                    )
                )
            }

            return Result.success(list)
        } catch (e: javax.crypto.AEADBadTagException) {
            return Result.failure(SecurityException("Incorrect PIN or password. Decryption failed."))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Imports the parsed contacts directly into the local app data (Room database)
     * as private contacts (isPrivate = true, androidContactId = null).
     */
    suspend fun importContactsIntoVault(contacts: List<PrivateContactExportDto>): Int = withContext(Dispatchers.IO) {
        var count = 0
        for (c in contacts) {
            // Strictly local: androidContactId = null, isPrivate = true
            val entity = ContactEntity(
                androidContactId = null,
                displayName = c.displayName,
                company = c.company,
                jobTitle = c.jobTitle,
                nickname = c.nickname,
                notes = c.notes,
                isPrivate = true
            )
            val newContactId = contactDao.insertContact(entity)

            val numberEntities = c.numbers.mapIndexed { idx, n ->
                ContactNumberEntity(
                    contactId = newContactId,
                    number = n.number,
                    normalizedNumber = n.number.filter { it.isDigit() || it == '+' },
                    label = n.label,
                    isPrimary = n.isPrimary || (idx == 0)
                )
            }
            if (numberEntities.isNotEmpty()) {
                contactDao.insertNumbers(numberEntities)
            }

            val emailEntities = c.emails.map { e ->
                ContactEmailEntity(
                    contactId = newContactId,
                    email = e.email,
                    label = e.label
                )
            }
            if (emailEntities.isNotEmpty()) {
                contactDao.insertEmails(emailEntities)
            }
            count++
        }
        count
    }

    private fun deriveAesKey(chars: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(chars, salt, PBKDF2_ITERATIONS, AES_KEY_SIZE_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        return factory.generateSecret(spec).encoded
    }
}
