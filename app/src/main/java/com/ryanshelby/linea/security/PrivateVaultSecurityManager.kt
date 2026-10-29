package com.ryanshelby.linea.security

import com.ryanshelby.linea.data.preferences.LineaPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Arrays
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ephemeral recovery authorization session granted only upon successful
 * verification of the security question answer via Argon2id.
 */
data class RecoverySession(
    val sessionId: String,
    val expiresAtMillis: Long,
    internal val recoveredMasterKey: ByteArray
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAtMillis

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecoverySession) return false
        return sessionId == other.sessionId && expiresAtMillis == other.expiresAtMillis
    }

    override fun hashCode(): Int {
        return sessionId.hashCode() * 31 + expiresAtMillis.hashCode()
    }
}

/**
 * Hardened, quantum-resistant security manager for Linea Private Safe & Contacts.
 *
 * Cryptographic Architecture & Design:
 * - Data Confidentiality (Quantum-Resistant):
 *   AES-256-GCM (Galois/Counter Mode with 128-bit authentication tag and unique 96-bit IV per encryption).
 *   Symmetric 256-bit keys provide 128 bits of quantum security against Grover's algorithm,
 *   satisfying NIST Post-Quantum Cryptography Level 5 symmetric equivalent requirements.
 *
 * - Password & Recovery Hashing (Memory-Hard Defense-in-Depth):
 *   Argon2id (RFC 9106) with 19 MiB memory matrix, 3 iterations, and 32-byte cryptographically
 *   secure salt. Argon2id's memory hardness provides resistance against GPU, ASIC, and quantum
 *   circuit brute-force attacks (constrained by quantum RAM and circuit depth).
 *   Security answers are NEVER stored in plaintext or with reversible encryption.
 *
 * - Minimum Answer Complexity:
 *   Enforces a strict minimum of 9 characters on all security answers.
 *
 * - Anti-Brute-Force & Rate Limiting:
 *   Shared multi-tier progressive exponential lockout across both PIN and recovery flows
 *   (Tier 1: 5 attempts = 30s; Tier 2: 8 attempts = 2m; Tier 3: 10 attempts = 10m).
 *   Constant-time byte comparisons (MessageDigest.isEqual) prevent timing side-channel attacks.
 *
 * - Cryptographically Bound PIN Recovery:
 *   Two-step authorization requiring answer validation before issuing an ephemeral 5-minute single-use
 *   recovery session. Unauthorized PIN resets and client-side bypasses are strictly prevented.
 *
 * - Memory Protection & Data Preservation:
 *   Sensitive master keys are zeroized upon lock or session expiry. Legacy PBKDF2 vaults are
 *   supported seamlessly and upgraded to Argon2id without data loss.
 */
@Singleton
class PrivateVaultSecurityManager(
    private val preferences: VaultPreferences
) {
    @Inject
    constructor(lineaPreferences: LineaPreferences) : this(lineaPreferences as VaultPreferences)

    companion object {
        private const val AES_KEY_SIZE_BITS = 256
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128

        // Argon2id Parameters (RFC 9106)
        const val ARGON2_PREFIX = "${'$'}argon2id${'$'}"
        private const val ARGON2_ITERATIONS = 3
        private const val ARGON2_MEMORY_KB = 19 * 1024 // 19 MiB
        private const val ARGON2_PARALLELISM = 1
        private const val ARGON2_HASH_LENGTH_BYTES = 64 // 512 bits
        private const val ARGON2_KEY_LENGTH_BYTES = 32  // 256 bits

        // Legacy PBKDF2 Parameters (for seamless backwards compatibility and data migration)
        private const val PBKDF2_ITERATIONS = 100_000
        private const val PBKDF2_KEY_LENGTH_BITS = 256
        private const val SALT_LENGTH_BYTES = 32

        const val PIN_LENGTH = 6
        const val MIN_SECURITY_ANSWER_LENGTH = 9
        const val RECOVERY_SESSION_DURATION_MS = 5 * 60 * 1000L // 5 minutes

        // Anti-Brute-Force Thresholds (in milliseconds)
        private const val LOCKOUT_TIER_1_ATTEMPTS = 5
        private const val LOCKOUT_TIER_1_DURATION_MS = 30_000L // 30 seconds

        private const val LOCKOUT_TIER_2_ATTEMPTS = 8
        private const val LOCKOUT_TIER_2_DURATION_MS = 120_000L // 2 minutes

        private const val LOCKOUT_TIER_3_ATTEMPTS = 10
        private const val LOCKOUT_TIER_3_DURATION_MS = 600_000L // 10 minutes
    }

    private val secureRandom = SecureRandom()
    private val scope = CoroutineScope(Dispatchers.Default)

    // In-memory master key: wiped with zeros on vault lock
    private var activeMasterKey: ByteArray? = null
    private var activeSessionPin: String? = null

    // Ephemeral single-use recovery session
    private var activeRecoverySession: RecoverySession? = null

    fun getActiveSessionPin(): String? = activeSessionPin

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    init {
        // Vault is ALWAYS locked on app initialization / cold restart
        lockVault()
    }

    suspend fun hasPinSet(): Boolean {
        return preferences.isVaultPinSet.first()
    }

    suspend fun getRecoveryQuestion(): String? {
        return preferences.vaultRecoveryQuestion.first()
    }

    suspend fun getLockoutRemainingSeconds(): Long {
        val lockoutUntil = preferences.vaultLockoutUntil.first()
        val now = System.currentTimeMillis()
        return if (lockoutUntil > now) (lockoutUntil - now) / 1000L else 0L
    }

    suspend fun getFailedAttempts(): Int {
        return preferences.vaultFailedAttempts.first()
    }

    /**
     * Initializes a new 6-digit PIN and Recovery Question with Argon2id and AES-256-GCM.
     * Prevents unauthorized overwrites if a PIN is already configured.
     */
    suspend fun setupInitialPin(pin: String, recoveryQuestion: String, recoveryAnswer: String): Result<Unit> {
        if (hasPinSet()) {
            return Result.failure(IllegalStateException("Vault PIN is already configured. Use PIN change or recovery flow."))
        }
        if (pin.length != PIN_LENGTH || !pin.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("PIN must be exactly $PIN_LENGTH digits"))
        }
        val normalizedAnswer = recoveryAnswer.trim().lowercase()
        if (recoveryQuestion.isBlank() || normalizedAnswer.isBlank()) {
            return Result.failure(IllegalArgumentException("Recovery question and answer are required"))
        }
        if (normalizedAnswer.length < MIN_SECURITY_ANSWER_LENGTH) {
            return Result.failure(IllegalArgumentException("Security answer must contain at least $MIN_SECURITY_ANSWER_LENGTH characters"))
        }

        try {
            // 1. Generate 256-bit cryptographically random master key
            val masterKey = generateRandomBytes(AES_KEY_SIZE_BITS / 8)

            // 2. Generate cryptographically secure salts
            val pinSalt = generateRandomBytes(SALT_LENGTH_BYTES)
            val recoverySalt = generateRandomBytes(SALT_LENGTH_BYTES)

            // 3. Derive PIN KEK & hash with Argon2id
            val pinKek = deriveKeyArgon2id(pin.toCharArray(), pinSalt, outputBytes = ARGON2_KEY_LENGTH_BYTES)
            val pinHash = hashWithArgon2id(pin.toCharArray(), pinSalt, outputBytes = ARGON2_HASH_LENGTH_BYTES)

            // 4. Derive Recovery KEK & hash with Argon2id (normalized answer)
            val recoveryKek = deriveKeyArgon2id(normalizedAnswer.toCharArray(), recoverySalt, outputBytes = ARGON2_KEY_LENGTH_BYTES)
            val recoveryHash = hashWithArgon2id(normalizedAnswer.toCharArray(), recoverySalt, outputBytes = ARGON2_HASH_LENGTH_BYTES)

            // 5. Encrypt master key with PIN KEK (AES-256-GCM)
            val encryptedMasterWithPin = encryptWithKey(masterKey, pinKek)

            // 6. Encrypt master key with Recovery KEK (AES-256-GCM)
            val encryptedMasterWithRecovery = encryptWithKey(masterKey, recoveryKek)

            // 7. Persist configuration
            preferences.saveVaultConfig(
                pinHash = ARGON2_PREFIX + toBase64(pinHash),
                pinSalt = toBase64(pinSalt),
                masterKeyEncrypted = toBase64(encryptedMasterWithPin),
                recoveryQuestion = recoveryQuestion.trim(),
                recoverySalt = toBase64(recoverySalt),
                recoveryHash = ARGON2_PREFIX + toBase64(recoveryHash),
                recoveryMasterKeyEncrypted = toBase64(encryptedMasterWithRecovery)
            )

            // Cache active master key and unlock
            activeMasterKey = masterKey
            activeSessionPin = pin
            _isVaultUnlocked.value = true
            preferences.setPrivateModeUnlocked(true)

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Verifies the 6-digit PIN with anti-brute-force rate limiting and unlocks the vault.
     * Supports both modern Argon2id credentials and legacy PBKDF2 credentials, automatically
     * migrating legacy credentials to Argon2id upon successful unlock.
     */
    suspend fun unlockWithPin(pin: String): Result<Unit> {
        val remainingLockout = getLockoutRemainingSeconds()
        if (remainingLockout > 0) {
            return Result.failure(SecurityException("Too many failed attempts. Locked out for ${remainingLockout}s."))
        }

        val pinHashStored = preferences.vaultPinHash.first()
        val pinSaltStored = preferences.vaultPinSalt.first()
        val masterKeyEncStored = preferences.vaultMasterKeyEncrypted.first()

        if (pinHashStored == null || pinSaltStored == null || masterKeyEncStored == null) {
            return Result.failure(IllegalStateException("No PIN has been configured."))
        }

        val saltBytes = fromBase64(pinSaltStored)
        val isArgon2 = pinHashStored.startsWith(ARGON2_PREFIX)
        val storedHashString = if (isArgon2) pinHashStored.removePrefix(ARGON2_PREFIX) else pinHashStored
        val storedHashBytes = fromBase64(storedHashString)

        val isMatch: Boolean
        val pinKek: ByteArray

        if (isArgon2) {
            val candidateHash = hashWithArgon2id(pin.toCharArray(), saltBytes, outputBytes = ARGON2_HASH_LENGTH_BYTES)
            isMatch = MessageDigest.isEqual(storedHashBytes, candidateHash)
            pinKek = deriveKeyArgon2id(pin.toCharArray(), saltBytes, outputBytes = ARGON2_KEY_LENGTH_BYTES)
        } else {
            // Legacy PBKDF2 hash verification for backwards compatibility
            val candidateHash = deriveKeyPbkdf2(pin.toCharArray(), saltBytes, outputBits = 512)
            isMatch = MessageDigest.isEqual(storedHashBytes, candidateHash)
            pinKek = deriveKeyPbkdf2(pin.toCharArray(), saltBytes, outputBits = 256)
        }

        if (!isMatch) {
            handleFailedAttempt()
            val attempts = getFailedAttempts()
            return Result.failure(SecurityException("Incorrect PIN. (Attempt $attempts)"))
        }

        // Correct PIN: Reset failed attempts
        preferences.resetVaultLockout()

        // Decrypt master key using PIN KEK
        val encryptedMasterBytes = fromBase64(masterKeyEncStored)
        val decryptedMaster = decryptWithKey(encryptedMasterBytes, pinKek)

        // If legacy PBKDF2, seamlessly upgrade to Argon2id at rest
        if (!isArgon2) {
            try {
                val newPinSalt = generateRandomBytes(SALT_LENGTH_BYTES)
                val newPinKek = deriveKeyArgon2id(pin.toCharArray(), newPinSalt, outputBytes = ARGON2_KEY_LENGTH_BYTES)
                val newPinHash = hashWithArgon2id(pin.toCharArray(), newPinSalt, outputBytes = ARGON2_HASH_LENGTH_BYTES)
                val newEncryptedMasterWithPin = encryptWithKey(decryptedMaster, newPinKek)

                preferences.updatePinCredentials(
                    pinHash = ARGON2_PREFIX + toBase64(newPinHash),
                    pinSalt = toBase64(newPinSalt),
                    masterKeyEncrypted = toBase64(newEncryptedMasterWithPin)
                )
            } catch (ignored: Exception) {
                // Non-fatal migration error
            }
        }

        activeMasterKey = decryptedMaster
        activeSessionPin = pin
        _isVaultUnlocked.value = true
        preferences.setPrivateModeUnlocked(true)

        return Result.success(Unit)
    }

    /**
     * Validates the security recovery answer with rate limiting and constant-time comparisons.
     * Upon success, issues an ephemeral single-use RecoverySession containing the recovered master key.
     */
    suspend fun verifyRecoveryAnswer(answer: String): Result<RecoverySession> {
        val remainingLockout = getLockoutRemainingSeconds()
        if (remainingLockout > 0) {
            return Result.failure(SecurityException("Too many failed attempts. Locked out for ${remainingLockout}s."))
        }

        val normalized = answer.trim().lowercase()
        if (normalized.length < MIN_SECURITY_ANSWER_LENGTH) {
            return Result.failure(IllegalArgumentException("Security answer must contain at least $MIN_SECURITY_ANSWER_LENGTH characters."))
        }

        val recoveryHashStored = preferences.vaultRecoveryHash.first()
        val recoverySaltStored = preferences.vaultRecoverySalt.first()
        val recoveryMasterEncStored = preferences.vaultRecoveryMasterKeyEncrypted.first()

        if (recoveryHashStored == null || recoverySaltStored == null || recoveryMasterEncStored == null) {
            return Result.failure(IllegalStateException("Recovery options are not configured."))
        }

        val saltBytes = fromBase64(recoverySaltStored)
        val isArgon2 = recoveryHashStored.startsWith(ARGON2_PREFIX)
        val storedHashString = if (isArgon2) recoveryHashStored.removePrefix(ARGON2_PREFIX) else recoveryHashStored
        val storedHashBytes = fromBase64(storedHashString)

        val isMatch: Boolean
        val recoveryKek: ByteArray

        if (isArgon2) {
            val candidateHash = hashWithArgon2id(normalized.toCharArray(), saltBytes, outputBytes = ARGON2_HASH_LENGTH_BYTES)
            isMatch = MessageDigest.isEqual(storedHashBytes, candidateHash)
            recoveryKek = deriveKeyArgon2id(normalized.toCharArray(), saltBytes, outputBytes = ARGON2_KEY_LENGTH_BYTES)
        } else {
            // Legacy PBKDF2 hash verification
            val candidateHash = deriveKeyPbkdf2(normalized.toCharArray(), saltBytes, outputBits = 512)
            isMatch = MessageDigest.isEqual(storedHashBytes, candidateHash)
            recoveryKek = deriveKeyPbkdf2(normalized.toCharArray(), saltBytes, outputBits = 256)
        }

        if (!isMatch) {
            handleFailedAttempt()
            val attempts = getFailedAttempts()
            return Result.failure(SecurityException("Incorrect recovery answer. (Attempt $attempts)"))
        }

        // Answer is correct! Reset failed attempts
        preferences.resetVaultLockout()

        // Decrypt master key using recovery KEK
        val masterKey = decryptWithKey(fromBase64(recoveryMasterEncStored), recoveryKek)

        // Invalidate any previous recovery session
        cancelRecoverySession()

        // Issue new ephemeral recovery session
        val session = RecoverySession(
            sessionId = toBase64(generateRandomBytes(32)),
            expiresAtMillis = System.currentTimeMillis() + RECOVERY_SESSION_DURATION_MS,
            recoveredMasterKey = masterKey
        )
        activeRecoverySession = session

        return Result.success(session)
    }

    /**
     * Resets the vault PIN using an authorized, unexpired RecoverySession.
     * Re-encrypts the existing master key with the new PIN so all existing vault contacts are preserved.
     */
    suspend fun resetPinWithRecoverySession(session: RecoverySession, newPin: String): Result<Unit> {
        val remainingLockout = getLockoutRemainingSeconds()
        if (remainingLockout > 0) {
            return Result.failure(SecurityException("Too many failed attempts. Locked out for ${remainingLockout}s."))
        }

        val active = activeRecoverySession
        if (active == null || active.sessionId != session.sessionId || active.isExpired()) {
            cancelRecoverySession()
            return Result.failure(SecurityException("Recovery session is invalid or expired. Please verify your answer again."))
        }

        if (newPin.length != PIN_LENGTH || !newPin.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("New PIN must be exactly $PIN_LENGTH digits"))
        }

        val masterKey = active.recoveredMasterKey
        val recoveryQuestion = preferences.vaultRecoveryQuestion.first() ?: "Security Question"
        val recoverySaltStored = preferences.vaultRecoverySalt.first()
        val recoveryHashStored = preferences.vaultRecoveryHash.first()
        val recoveryMasterEncStored = preferences.vaultRecoveryMasterKeyEncrypted.first()

        if (recoverySaltStored == null || recoveryHashStored == null || recoveryMasterEncStored == null) {
            return Result.failure(IllegalStateException("Recovery options are not configured."))
        }

        try {
            // Re-encrypt master key with new PIN using Argon2id
            val newPinSalt = generateRandomBytes(SALT_LENGTH_BYTES)
            val newPinKek = deriveKeyArgon2id(newPin.toCharArray(), newPinSalt, outputBytes = ARGON2_KEY_LENGTH_BYTES)
            val newPinHash = hashWithArgon2id(newPin.toCharArray(), newPinSalt, outputBytes = ARGON2_HASH_LENGTH_BYTES)
            val newEncryptedMasterWithPin = encryptWithKey(masterKey, newPinKek)

            preferences.saveVaultConfig(
                pinHash = ARGON2_PREFIX + toBase64(newPinHash),
                pinSalt = toBase64(newPinSalt),
                masterKeyEncrypted = toBase64(newEncryptedMasterWithPin),
                recoveryQuestion = recoveryQuestion,
                recoverySalt = recoverySaltStored,
                recoveryHash = recoveryHashStored,
                recoveryMasterKeyEncrypted = recoveryMasterEncStored
            )

            // Clear session and unlock vault
            activeRecoverySession = null
            activeMasterKey = masterKey
            activeSessionPin = newPin
            _isVaultUnlocked.value = true
            preferences.setPrivateModeUnlocked(true)

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Atomically validates recovery answer and resets PIN in a single operation.
     * Enforces rate limiting, 9-character minimum answer length, and master key re-encryption.
     */
    suspend fun recoverWithSecurityAnswer(answer: String, newPin: String): Result<Unit> {
        val verifyResult = verifyRecoveryAnswer(answer)
        if (verifyResult.isFailure) {
            return Result.failure(verifyResult.exceptionOrNull()!!)
        }
        val session = verifyResult.getOrThrow()
        return resetPinWithRecoverySession(session, newPin)
    }

    /**
     * Invalidates and zeroes any active recovery session in memory.
     */
    fun cancelRecoverySession() {
        activeRecoverySession?.recoveredMasterKey?.let { Arrays.fill(it, 0.toByte()) }
        activeRecoverySession = null
    }

    /**
     * Changes 6-digit PIN with old PIN verification.
     */
    suspend fun changePin(oldPin: String, newPin: String): Result<Unit> {
        if (newPin.length != PIN_LENGTH || !newPin.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("New PIN must be exactly $PIN_LENGTH digits"))
        }

        // Verify old PIN
        val unlockResult = unlockWithPin(oldPin)
        if (unlockResult.isFailure) {
            return unlockResult
        }

        val masterKey = activeMasterKey ?: return Result.failure(IllegalStateException("Vault key unavailable"))

        try {
            val newPinSalt = generateRandomBytes(SALT_LENGTH_BYTES)
            val newPinKek = deriveKeyArgon2id(newPin.toCharArray(), newPinSalt, outputBytes = ARGON2_KEY_LENGTH_BYTES)
            val newPinHash = hashWithArgon2id(newPin.toCharArray(), newPinSalt, outputBytes = ARGON2_HASH_LENGTH_BYTES)
            val newEncryptedMasterWithPin = encryptWithKey(masterKey, newPinKek)

            preferences.updatePinCredentials(
                pinHash = ARGON2_PREFIX + toBase64(newPinHash),
                pinSalt = toBase64(newPinSalt),
                masterKeyEncrypted = toBase64(newEncryptedMasterWithPin)
            )

            activeSessionPin = newPin
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Changes the Recovery Question and Answer (requires current PIN and minimum 9 characters).
     */
    suspend fun changeRecoveryQuestion(currentPin: String, newQuestion: String, newAnswer: String): Result<Unit> {
        val unlockResult = unlockWithPin(currentPin)
        if (unlockResult.isFailure) return unlockResult

        val normalizedAnswer = newAnswer.trim().lowercase()
        if (newQuestion.isBlank() || normalizedAnswer.isBlank()) {
            return Result.failure(IllegalArgumentException("Recovery question and answer are required"))
        }
        if (normalizedAnswer.length < MIN_SECURITY_ANSWER_LENGTH) {
            return Result.failure(IllegalArgumentException("Security answer must contain at least $MIN_SECURITY_ANSWER_LENGTH characters"))
        }

        val masterKey = activeMasterKey ?: return Result.failure(IllegalStateException("Vault key unavailable"))

        try {
            val recoverySalt = generateRandomBytes(SALT_LENGTH_BYTES)
            val recoveryKek = deriveKeyArgon2id(normalizedAnswer.toCharArray(), recoverySalt, outputBytes = ARGON2_KEY_LENGTH_BYTES)
            val recoveryHash = hashWithArgon2id(normalizedAnswer.toCharArray(), recoverySalt, outputBytes = ARGON2_HASH_LENGTH_BYTES)
            val encryptedMasterWithRecovery = encryptWithKey(masterKey, recoveryKek)

            preferences.updateRecoveryCredentials(
                question = newQuestion.trim(),
                salt = toBase64(recoverySalt),
                hash = ARGON2_PREFIX + toBase64(recoveryHash),
                masterKeyEnc = toBase64(encryptedMasterWithRecovery)
            )

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Wipes active master key and recovery session from memory and locks the vault.
     */
    fun lockVault() {
        cancelRecoverySession()
        activeMasterKey?.let { Arrays.fill(it, 0.toByte()) }
        activeMasterKey = null
        activeSessionPin = null
        _isVaultUnlocked.value = false
        scope.launch {
            preferences.setPrivateModeUnlocked(false)
        }
    }

    /**
     * Encrypts private contact payload with the quantum-proof master key (AES-256-GCM).
     */
    fun encryptVaultPayload(plaintext: String): String? {
        val key = activeMasterKey ?: return null
        val secretKey = SecretKeySpec(key, "AES")
        val iv = generateRandomBytes(GCM_IV_LENGTH_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Return combined [IV (12) + Ciphertext + Tag] Base64 encoded
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return toBase64(combined)
    }

    /**
     * Decrypts private contact payload with the active master key.
     */
    fun decryptVaultPayload(encryptedBase64: String): String? {
        val key = activeMasterKey ?: return null
        val secretKey = SecretKeySpec(key, "AES")
        val combined = fromBase64(encryptedBase64)
        if (combined.size <= GCM_IV_LENGTH_BYTES) return null

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        val ciphertext = ByteArray(combined.size - GCM_IV_LENGTH_BYTES)
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES)
        System.arraycopy(combined, GCM_IV_LENGTH_BYTES, ciphertext, 0, ciphertext.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private suspend fun handleFailedAttempt() {
        val currentFailed = preferences.vaultFailedAttempts.first() + 1
        var lockoutUntil = 0L
        val now = System.currentTimeMillis()

        if (currentFailed >= LOCKOUT_TIER_3_ATTEMPTS) {
            lockoutUntil = now + LOCKOUT_TIER_3_DURATION_MS
        } else if (currentFailed >= LOCKOUT_TIER_2_ATTEMPTS) {
            lockoutUntil = now + LOCKOUT_TIER_2_DURATION_MS
        } else if (currentFailed >= LOCKOUT_TIER_1_ATTEMPTS) {
            lockoutUntil = now + LOCKOUT_TIER_1_DURATION_MS
        }

        // Progressive timing delay to mitigate automated attack scripts
        delay(minOf(currentFailed * 50L, 500L))

        preferences.recordFailedVaultAttempt(currentFailed, lockoutUntil)
    }

    private fun hashWithArgon2id(
        chars: CharArray,
        salt: ByteArray,
        outputBytes: Int = ARGON2_HASH_LENGTH_BYTES
    ): ByteArray {
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(ARGON2_ITERATIONS)
            .withMemoryAsKB(ARGON2_MEMORY_KB)
            .withParallelism(ARGON2_PARALLELISM)
            .withSalt(salt)
            .build()

        val generator = Argon2BytesGenerator()
        generator.init(params)
        val result = ByteArray(outputBytes)
        generator.generateBytes(chars, result)
        return result
    }

    private fun deriveKeyArgon2id(
        chars: CharArray,
        salt: ByteArray,
        outputBytes: Int = ARGON2_KEY_LENGTH_BYTES
    ): ByteArray {
        return hashWithArgon2id(chars, salt, outputBytes)
    }

    private fun deriveKeyPbkdf2(
        chars: CharArray,
        salt: ByteArray,
        outputBits: Int = PBKDF2_KEY_LENGTH_BITS
    ): ByteArray {
        val spec = PBEKeySpec(chars, salt, PBKDF2_ITERATIONS, outputBits)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        return factory.generateSecret(spec).encoded
    }

    private fun encryptWithKey(data: ByteArray, keyBytes: ByteArray): ByteArray {
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val iv = generateRandomBytes(GCM_IV_LENGTH_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(data)

        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return combined
    }

    private fun decryptWithKey(combined: ByteArray, keyBytes: ByteArray): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        val ciphertext = ByteArray(combined.size - GCM_IV_LENGTH_BYTES)
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES)
        System.arraycopy(combined, GCM_IV_LENGTH_BYTES, ciphertext, 0, ciphertext.size)

        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun generateRandomBytes(count: Int): ByteArray {
        val bytes = ByteArray(count)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    private fun toBase64(bytes: ByteArray): String {
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun fromBase64(str: String): ByteArray {
        return Base64.getDecoder().decode(str)
    }
}
