package com.ryanshelby.linea

import com.ryanshelby.linea.security.PrivateVaultSecurityManager
import com.ryanshelby.linea.security.RecoverySession
import com.ryanshelby.linea.security.VaultPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class PrivateVaultSecurityManagerTest {

    private lateinit var fakePreferences: FakeVaultPreferences
    private lateinit var securityManager: PrivateVaultSecurityManager

    class FakeVaultPreferences : VaultPreferences {
        val isVaultPinSetFlow = MutableStateFlow(false)
        override val isVaultPinSet: Flow<Boolean> = isVaultPinSetFlow.asStateFlow()

        val vaultPinHashFlow = MutableStateFlow<String?>(null)
        override val vaultPinHash: Flow<String?> = vaultPinHashFlow.asStateFlow()

        val vaultPinSaltFlow = MutableStateFlow<String?>(null)
        override val vaultPinSalt: Flow<String?> = vaultPinSaltFlow.asStateFlow()

        val vaultMasterKeyEncryptedFlow = MutableStateFlow<String?>(null)
        override val vaultMasterKeyEncrypted: Flow<String?> = vaultMasterKeyEncryptedFlow.asStateFlow()

        val vaultRecoveryQuestionFlow = MutableStateFlow<String?>(null)
        override val vaultRecoveryQuestion: Flow<String?> = vaultRecoveryQuestionFlow.asStateFlow()

        val vaultRecoverySaltFlow = MutableStateFlow<String?>(null)
        override val vaultRecoverySalt: Flow<String?> = vaultRecoverySaltFlow.asStateFlow()

        val vaultRecoveryHashFlow = MutableStateFlow<String?>(null)
        override val vaultRecoveryHash: Flow<String?> = vaultRecoveryHashFlow.asStateFlow()

        val vaultRecoveryMasterKeyEncryptedFlow = MutableStateFlow<String?>(null)
        override val vaultRecoveryMasterKeyEncrypted: Flow<String?> = vaultRecoveryMasterKeyEncryptedFlow.asStateFlow()

        val vaultFailedAttemptsFlow = MutableStateFlow(0)
        override val vaultFailedAttempts: Flow<Int> = vaultFailedAttemptsFlow.asStateFlow()

        val vaultLockoutUntilFlow = MutableStateFlow(0L)
        override val vaultLockoutUntil: Flow<Long> = vaultLockoutUntilFlow.asStateFlow()

        val privateModeUnlockedFlow = MutableStateFlow(false)

        override suspend fun saveVaultConfig(
            pinHash: String,
            pinSalt: String,
            masterKeyEncrypted: String,
            recoveryQuestion: String,
            recoverySalt: String,
            recoveryHash: String,
            recoveryMasterKeyEncrypted: String
        ) {
            isVaultPinSetFlow.value = true
            vaultPinHashFlow.value = pinHash
            vaultPinSaltFlow.value = pinSalt
            vaultMasterKeyEncryptedFlow.value = masterKeyEncrypted
            vaultRecoveryQuestionFlow.value = recoveryQuestion
            vaultRecoverySaltFlow.value = recoverySalt
            vaultRecoveryHashFlow.value = recoveryHash
            vaultRecoveryMasterKeyEncryptedFlow.value = recoveryMasterKeyEncrypted
            vaultFailedAttemptsFlow.value = 0
            vaultLockoutUntilFlow.value = 0L
        }

        override suspend fun updatePinCredentials(
            pinHash: String,
            pinSalt: String,
            masterKeyEncrypted: String
        ) {
            isVaultPinSetFlow.value = true
            vaultPinHashFlow.value = pinHash
            vaultPinSaltFlow.value = pinSalt
            vaultMasterKeyEncryptedFlow.value = masterKeyEncrypted
            vaultFailedAttemptsFlow.value = 0
            vaultLockoutUntilFlow.value = 0L
        }

        override suspend fun updateRecoveryCredentials(
            question: String,
            salt: String,
            hash: String,
            masterKeyEnc: String
        ) {
            vaultRecoveryQuestionFlow.value = question
            vaultRecoverySaltFlow.value = salt
            vaultRecoveryHashFlow.value = hash
            vaultRecoveryMasterKeyEncryptedFlow.value = masterKeyEnc
        }

        override suspend fun recordFailedVaultAttempt(failedAttempts: Int, lockoutUntil: Long) {
            vaultFailedAttemptsFlow.value = failedAttempts
            vaultLockoutUntilFlow.value = lockoutUntil
        }

        override suspend fun resetVaultLockout() {
            vaultFailedAttemptsFlow.value = 0
            vaultLockoutUntilFlow.value = 0L
        }

        override suspend fun setPrivateModeUnlocked(unlocked: Boolean) {
            privateModeUnlockedFlow.value = unlocked
        }
    }

    @Before
    fun setUp() {
        fakePreferences = FakeVaultPreferences()
        securityManager = PrivateVaultSecurityManager(fakePreferences)
    }

    @Test
    fun testPinValidation_requiresExactlySixDigits() = runBlocking {
        // Less than 6 digits
        val shortResult = securityManager.setupInitialPin("12345", "Favorite City?", "Tokyo Metropolis")
        assertTrue(shortResult.isFailure)
        assertEquals("PIN must be exactly 6 digits", shortResult.exceptionOrNull()?.message)

        // More than 6 digits
        val longResult = securityManager.setupInitialPin("1234567", "Favorite City?", "Tokyo Metropolis")
        assertTrue(longResult.isFailure)

        // 4 digits (the old default PIN) must be strictly rejected
        val oldDefaultResult = securityManager.setupInitialPin("1234", "Favorite City?", "Tokyo Metropolis")
        assertTrue(oldDefaultResult.isFailure)

        // Non-numeric characters
        val alphaResult = securityManager.setupInitialPin("12345a", "Favorite City?", "Tokyo Metropolis")
        assertTrue(alphaResult.isFailure)

        // Exactly 6 digits with valid >= 9 char answer succeeds
        val validResult = securityManager.setupInitialPin("849201", "Favorite City?", "Tokyo Metropolis")
        assertTrue(validResult.isSuccess)
    }

    @Test
    fun testInitialSetup_requiresQuestionAndMinimumNineCharAnswer() = runBlocking {
        // Blank question
        val blankQuestion = securityManager.setupInitialPin("123456", "", "San Francisco")
        assertTrue(blankQuestion.isFailure)

        // Blank answer
        val blankAnswer = securityManager.setupInitialPin("123456", "Favorite City?", "  ")
        assertTrue(blankAnswer.isFailure)

        // Answer < 9 characters strictly rejected
        val shortAnswer1 = securityManager.setupInitialPin("123456", "Favorite City?", "Tokyo")
        assertTrue(shortAnswer1.isFailure)
        assertTrue(shortAnswer1.exceptionOrNull()?.message?.contains("9 characters") == true)

        val shortAnswer2 = securityManager.setupInitialPin("123456", "Favorite City?", "12345678")
        assertTrue(shortAnswer2.isFailure)
        assertTrue(shortAnswer2.exceptionOrNull()?.message?.contains("9 characters") == true)

        // Answer >= 9 characters succeeds
        val validAnswer = securityManager.setupInitialPin("123456", "Favorite City?", "San Francisco")
        assertTrue(validAnswer.isSuccess)
    }

    @Test
    fun testSetupInitialPin_cannotBeCalledWhenPinAlreadyConfigured() = runBlocking {
        val firstSetup = securityManager.setupInitialPin("123456", "Security Question", "ValidAnswer123")
        assertTrue(firstSetup.isSuccess)

        // Attempting to call setupInitialPin again when PIN is already set must be rejected
        val secondSetup = securityManager.setupInitialPin("654321", "Another Question", "AnotherValidAnswer")
        assertTrue(secondSetup.isFailure)
        assertTrue(secondSetup.exceptionOrNull() is IllegalStateException)
        assertTrue(secondSetup.exceptionOrNull()?.message?.contains("already configured") == true)
    }

    @Test
    fun testSetupAndUnlock_withCorrectPin() = runBlocking {
        val pin = "654321"
        val question = "What is your primary pet's name?"
        val answer = "Luna The Golden"

        val setupResult = securityManager.setupInitialPin(pin, question, answer)
        assertTrue(setupResult.isSuccess)
        assertTrue(securityManager.isVaultUnlocked.value)
        assertTrue(fakePreferences.isVaultPinSetFlow.value)
        assertEquals(question, securityManager.getRecoveryQuestion())

        // Verify Argon2id prefix is saved in preferences
        val savedPinHash = fakePreferences.vaultPinHashFlow.value
        assertNotNull(savedPinHash)
        assertTrue("Pin hash must be prefixed with Argon2id marker", savedPinHash!!.startsWith(PrivateVaultSecurityManager.ARGON2_PREFIX))

        val savedRecoveryHash = fakePreferences.vaultRecoveryHashFlow.value
        assertNotNull(savedRecoveryHash)
        assertTrue("Recovery hash must be prefixed with Argon2id marker", savedRecoveryHash!!.startsWith(PrivateVaultSecurityManager.ARGON2_PREFIX))

        // Lock the vault
        securityManager.lockVault()
        assertFalse(securityManager.isVaultUnlocked.value)

        // Unlock with correct PIN
        val unlockResult = securityManager.unlockWithPin(pin)
        assertTrue(unlockResult.isSuccess)
        assertTrue(securityManager.isVaultUnlocked.value)
    }

    @Test
    fun testUnlock_withIncorrectPin_triggersRateLimiting() = runBlocking {
        val pin = "112233"
        securityManager.setupInitialPin(pin, "Question", "AnswerNineChars")
        securityManager.lockVault()

        // 1 to 4 failed attempts should not trigger lockout
        for (i in 1..4) {
            val result = securityManager.unlockWithPin("999999")
            assertTrue(result.isFailure)
            assertEquals(i, securityManager.getFailedAttempts())
            assertEquals(0L, securityManager.getLockoutRemainingSeconds())
        }

        // 5th failed attempt triggers Tier 1 lockout (30 seconds)
        val fifthAttempt = securityManager.unlockWithPin("999999")
        assertTrue(fifthAttempt.isFailure)
        assertEquals(5, securityManager.getFailedAttempts())
        val remainingLockout = securityManager.getLockoutRemainingSeconds()
        assertTrue("Lockout should be approximately 30s", remainingLockout in 25..30)

        // An attempt while locked out is rejected immediately
        val lockedOutAttempt = securityManager.unlockWithPin(pin)
        assertTrue(lockedOutAttempt.isFailure)
        assertTrue(lockedOutAttempt.exceptionOrNull()?.message?.contains("Locked out") == true)
    }

    @Test
    fun testSuccessfulUnlock_resetsFailedAttempts() = runBlocking {
        val pin = "554433"
        securityManager.setupInitialPin(pin, "Question", "AnswerNineChars")
        securityManager.lockVault()

        // 3 failed attempts
        repeat(3) { securityManager.unlockWithPin("000000") }
        assertEquals(3, securityManager.getFailedAttempts())

        // Unlock with correct PIN
        val success = securityManager.unlockWithPin(pin)
        assertTrue(success.isSuccess)
        assertEquals(0, securityManager.getFailedAttempts())
        assertEquals(0L, securityManager.getLockoutRemainingSeconds())
    }

    @Test
    fun testRecovery_requiresMinimumNineCharAnswer() = runBlocking {
        securityManager.setupInitialPin("123456", "Question", "ValidSecurityAnswer")
        securityManager.lockVault()

        // Less than 9 characters in verifyRecoveryAnswer rejected
        val shortVerify = securityManager.verifyRecoveryAnswer("Short")
        assertTrue(shortVerify.isFailure)
        assertTrue(shortVerify.exceptionOrNull()?.message?.contains("9 characters") == true)

        // Less than 9 characters in recoverWithSecurityAnswer rejected
        val shortRecover = securityManager.recoverWithSecurityAnswer("Short", "654321")
        assertTrue(shortRecover.isFailure)
        assertTrue(shortRecover.exceptionOrNull()?.message?.contains("9 characters") == true)
    }

    @Test
    fun testRecovery_withIncorrectAnswer_isRejectedAndTriggersRateLimiting() = runBlocking {
        securityManager.setupInitialPin("123456", "In which city were you born?", "San Francisco Bay")
        securityManager.lockVault()

        // Incorrect answer with >= 9 characters
        val wrongAnswerResult = securityManager.verifyRecoveryAnswer("Los Angeles Metro")
        assertTrue(wrongAnswerResult.isFailure)
        assertEquals("Incorrect recovery answer. (Attempt 1)", wrongAnswerResult.exceptionOrNull()?.message)
        assertEquals(1, securityManager.getFailedAttempts())

        // Repeat until lockout
        repeat(3) {
            val res = securityManager.verifyRecoveryAnswer("WrongAnswerHere")
            assertTrue(res.isFailure)
        }
        assertEquals(4, securityManager.getFailedAttempts())

        // 5th attempt triggers lockout
        val fifthRes = securityManager.verifyRecoveryAnswer("WrongAnswerHere")
        assertTrue(fifthRes.isFailure)
        assertEquals(5, securityManager.getFailedAttempts())
        assertTrue(securityManager.getLockoutRemainingSeconds() > 0)
    }

    @Test
    fun testRecovery_lockedOutState_rejectsRecoveryAttempts() = runBlocking {
        securityManager.setupInitialPin("123456", "Question", "ValidAnswerNine")
        securityManager.lockVault()

        // Lockout by failed attempts
        repeat(5) { securityManager.unlockWithPin("000000") }
        assertTrue(securityManager.getLockoutRemainingSeconds() > 0)

        // Even with correct recovery answer, attempt while locked out must be blocked
        val blockedRecovery = securityManager.verifyRecoveryAnswer("ValidAnswerNine")
        assertTrue(blockedRecovery.isFailure)
        assertTrue(blockedRecovery.exceptionOrNull()?.message?.contains("Locked out") == true)
    }

    @Test
    fun testRecoveryFlow_twoStepSessionVerificationAndReset() = runBlocking {
        val originalPin = "123456"
        val question = "In which city were you born?"
        val answer = "San Francisco"

        securityManager.setupInitialPin(originalPin, question, answer)

        // Encrypt test payload with initial master key
        val secretPayload = """{"contact":"007","agent":"James Bond"}"""
        val encryptedBefore = securityManager.encryptVaultPayload(secretPayload)
        assertNotNull(encryptedBefore)

        securityManager.lockVault()

        // Step 1: Verify answer and obtain RecoverySession
        val verifyResult = securityManager.verifyRecoveryAnswer("  sAn FrAnCiScO  ")
        assertTrue(verifyResult.isSuccess)
        val session = verifyResult.getOrNull()
        assertNotNull(session)
        assertFalse(session!!.isExpired())

        // Step 2: Reset PIN with valid RecoverySession
        val newPin = "987654"
        val resetResult = securityManager.resetPinWithRecoverySession(session, newPin)
        assertTrue(resetResult.isSuccess)
        assertTrue(securityManager.isVaultUnlocked.value)

        // Existing payload can still be decrypted with the recovered master key!
        val decryptedAfter = securityManager.decryptVaultPayload(encryptedBefore!!)
        assertEquals(secretPayload, decryptedAfter)

        // Lock vault and verify old PIN fails while new PIN succeeds
        securityManager.lockVault()
        val oldPinUnlock = securityManager.unlockWithPin(originalPin)
        assertTrue(oldPinUnlock.isFailure)

        val newPinUnlock = securityManager.unlockWithPin(newPin)
        assertTrue(newPinUnlock.isSuccess)
    }

    @Test
    fun testRecoverySession_invalidOrExpiredSession_isRejected() = runBlocking {
        securityManager.setupInitialPin("123456", "Question", "ValidAnswerNine")
        securityManager.lockVault()

        // Fake / tampered session
        val fakeSession = RecoverySession(
            sessionId = "forged_session_id_12345",
            expiresAtMillis = System.currentTimeMillis() + 60000L,
            recoveredMasterKey = ByteArray(32)
        )
        val resetResult = securityManager.resetPinWithRecoverySession(fakeSession, "654321")
        assertTrue(resetResult.isFailure)
        assertTrue(resetResult.exceptionOrNull()?.message?.contains("invalid or expired") == true)

        // Expired session
        val expiredSession = RecoverySession(
            sessionId = "expired_session_id",
            expiresAtMillis = System.currentTimeMillis() - 1000L,
            recoveredMasterKey = ByteArray(32)
        )
        val expiredResult = securityManager.resetPinWithRecoverySession(expiredSession, "654321")
        assertTrue(expiredResult.isFailure)
    }

    @Test
    fun testRecoveryFlow_atomicDirectApiWithCorrectAnswer() = runBlocking {
        val originalPin = "111222"
        val answer = "ArtisanalNotebook"
        securityManager.setupInitialPin(originalPin, "Security Question", answer)
        securityManager.lockVault()

        val newPin = "333444"
        val recoveryResult = securityManager.recoverWithSecurityAnswer("  artisanalnotebook  ", newPin)
        assertTrue(recoveryResult.isSuccess)

        // Lock and verify
        securityManager.lockVault()
        assertTrue(securityManager.unlockWithPin(newPin).isSuccess)
        assertFalse(securityManager.unlockWithPin(originalPin).isSuccess)
    }

    @Test
    fun testChangePin_requiresOldPinVerification() = runBlocking {
        val pin = "123456"
        val newPin = "789012"
        securityManager.setupInitialPin(pin, "Question", "ValidSecurityAnswer")

        // Incorrect old PIN
        val failChange = securityManager.changePin("000000", newPin)
        assertTrue(failChange.isFailure)

        // Correct old PIN
        val successChange = securityManager.changePin(pin, newPin)
        assertTrue(successChange.isSuccess)

        // Verify unlocking with new PIN works
        securityManager.lockVault()
        assertTrue(securityManager.unlockWithPin(newPin).isSuccess)
    }

    @Test
    fun testChangeRecoveryQuestion() = runBlocking {
        val pin = "654321"
        securityManager.setupInitialPin(pin, "Old Question", "OldAnswerNine")

        val newQuestion = "What was your childhood nickname?"
        val newAnswer = "Sparky The Dog"
        val changeResult = securityManager.changeRecoveryQuestion(pin, newQuestion, newAnswer)
        assertTrue(changeResult.isSuccess)
        assertEquals(newQuestion, securityManager.getRecoveryQuestion())

        // Verify recovery works with new answer
        securityManager.lockVault()
        val recoveryResult = securityManager.recoverWithSecurityAnswer("sparky the dog", "111222")
        assertTrue(recoveryResult.isSuccess)
    }

    @Test
    fun testLegacyMigration_preservesExistingPbkdf2VaultAndUpgradesToArgon2id() = runBlocking {
        // Construct a legacy PBKDF2 vault configuration (no $argon2id$ prefix)
        val legacyPin = "123456"
        val legacyAnswer = "san francisco"
        val random = SecureRandom()
        val masterKey = ByteArray(32).also { random.nextBytes(it) }
        val pinSalt = ByteArray(32).also { random.nextBytes(it) }
        val recoverySalt = ByteArray(32).also { random.nextBytes(it) }

        // PBKDF2 KEKs & Hashes
        fun derivePbkdf2(chars: CharArray, salt: ByteArray, bits: Int): ByteArray {
            val spec = PBEKeySpec(chars, salt, 100_000, bits)
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512").generateSecret(spec).encoded
        }

        fun encryptAesGcm(data: ByteArray, key: ByteArray): ByteArray {
            val iv = ByteArray(12).also { random.nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
            val ct = cipher.doFinal(data)
            return iv + ct
        }

        val pinKek = derivePbkdf2(legacyPin.toCharArray(), pinSalt, 256)
        val pinHash = derivePbkdf2(legacyPin.toCharArray(), pinSalt, 512)
        val recoveryKek = derivePbkdf2(legacyAnswer.toCharArray(), recoverySalt, 256)
        val recoveryHash = derivePbkdf2(legacyAnswer.toCharArray(), recoverySalt, 512)

        val b64 = Base64.getEncoder()
        val encMasterPin = encryptAesGcm(masterKey, pinKek)
        val encMasterRecovery = encryptAesGcm(masterKey, recoveryKek)

        // Seed fake preferences with legacy PBKDF2 configuration (no $argon2id$ prefix)
        fakePreferences.saveVaultConfig(
            pinHash = b64.encodeToString(pinHash),
            pinSalt = b64.encodeToString(pinSalt),
            masterKeyEncrypted = b64.encodeToString(encMasterPin),
            recoveryQuestion = "City of birth?",
            recoverySalt = b64.encodeToString(recoverySalt),
            recoveryHash = b64.encodeToString(recoveryHash),
            recoveryMasterKeyEncrypted = b64.encodeToString(encMasterRecovery)
        )

        // 1. Verify legacy unlock works seamlessly
        val unlockResult = securityManager.unlockWithPin(legacyPin)
        assertTrue("Legacy PBKDF2 vault must unlock successfully", unlockResult.isSuccess)

        // 2. Verify credentials were automatically upgraded to Argon2id at rest!
        val upgradedPinHash = fakePreferences.vaultPinHashFlow.value
        assertNotNull(upgradedPinHash)
        assertTrue("Migrated pin hash must now have Argon2id prefix", upgradedPinHash!!.startsWith(PrivateVaultSecurityManager.ARGON2_PREFIX))

        // 3. Verify encryption/decryption works with the preserved master key
        val payload = "Confidential Contact"
        val ct = securityManager.encryptVaultPayload(payload)
        assertEquals(payload, securityManager.decryptVaultPayload(ct!!))
    }

    @Test
    fun testQuantumProofPayloadEncryptionAndDecryption() = runBlocking {
        val pin = "123987"
        securityManager.setupInitialPin(pin, "Question", "AnswerNineChars")

        val privateContactData = """{"name":"John Wick","phone":"+15550199999","notes":"High Table"}"""

        // Encrypt data while unlocked
        val ciphertext = securityManager.encryptVaultPayload(privateContactData)
        assertNotNull(ciphertext)
        assertFalse(ciphertext!!.contains("John Wick"))

        // Decrypt data while unlocked
        val decrypted = securityManager.decryptVaultPayload(ciphertext)
        assertEquals(privateContactData, decrypted)

        // Lock the vault -> master key wiped from memory
        securityManager.lockVault()
        assertNull(securityManager.encryptVaultPayload(privateContactData))
        assertNull(securityManager.decryptVaultPayload(ciphertext))

        // Unlock vault again -> decryption works seamlessly
        securityManager.unlockWithPin(pin)
        val decryptedAfterUnlock = securityManager.decryptVaultPayload(ciphertext)
        assertEquals(privateContactData, decryptedAfterUnlock)
    }

    @Test
    fun testMemorySafety_lockVaultWipesKeys() = runBlocking {
        securityManager.setupInitialPin("123456", "Question", "ValidAnswerNine")
        assertTrue(securityManager.isVaultUnlocked.value)
        assertNotNull(securityManager.getActiveSessionPin())

        // Lock vault
        securityManager.lockVault()
        assertFalse(securityManager.isVaultUnlocked.value)
        assertNull(securityManager.getActiveSessionPin())
        assertNull(securityManager.encryptVaultPayload("Test"))
    }
}
