package com.ryanshelby.linea

import com.ryanshelby.linea.security.PrivateVaultSecurityManager
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
        val shortResult = securityManager.setupInitialPin("12345", "Favorite City?", "Tokyo")
        assertTrue(shortResult.isFailure)
        assertEquals("PIN must be exactly 6 digits", shortResult.exceptionOrNull()?.message)

        // More than 6 digits
        val longResult = securityManager.setupInitialPin("1234567", "Favorite City?", "Tokyo")
        assertTrue(longResult.isFailure)

        // 4 digits (the old default PIN) must be strictly rejected
        val oldDefaultResult = securityManager.setupInitialPin("1234", "Favorite City?", "Tokyo")
        assertTrue(oldDefaultResult.isFailure)

        // Non-numeric characters
        val alphaResult = securityManager.setupInitialPin("12345a", "Favorite City?", "Tokyo")
        assertTrue(alphaResult.isFailure)

        // Exactly 6 digits succeeds
        val validResult = securityManager.setupInitialPin("849201", "Favorite City?", "Tokyo")
        assertTrue(validResult.isSuccess)
    }

    @Test
    fun testInitialSetup_requiresQuestionAndAnswer() = runBlocking {
        val blankQuestion = securityManager.setupInitialPin("123456", "", "Tokyo")
        assertTrue(blankQuestion.isFailure)

        val blankAnswer = securityManager.setupInitialPin("123456", "Favorite City?", "  ")
        assertTrue(blankAnswer.isFailure)
    }

    @Test
    fun testSetupAndUnlock_withCorrectPin() = runBlocking {
        val pin = "654321"
        val question = "What is your primary pet's name?"
        val answer = "Luna"

        val setupResult = securityManager.setupInitialPin(pin, question, answer)
        assertTrue(setupResult.isSuccess)
        assertTrue(securityManager.isVaultUnlocked.value)
        assertTrue(fakePreferences.isVaultPinSetFlow.value)
        assertEquals(question, securityManager.getRecoveryQuestion())

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
        securityManager.setupInitialPin(pin, "Question", "Answer")
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
        securityManager.setupInitialPin(pin, "Question", "Answer")
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
    fun testRecoveryFlow_normalizesAnswerAndSetsNewPin() = runBlocking {
        val originalPin = "123456"
        val question = "In which city were you born?"
        val answer = "San Francisco"

        securityManager.setupInitialPin(originalPin, question, answer)
        securityManager.lockVault()

        // Incorrect answer
        val wrongAnswerResult = securityManager.recoverWithSecurityAnswer("New York", "654321")
        assertTrue(wrongAnswerResult.isFailure)
        assertEquals("Incorrect recovery answer.", wrongAnswerResult.exceptionOrNull()?.message)

        // Invalid new PIN
        val invalidNewPinResult = securityManager.recoverWithSecurityAnswer("SAN FRANCISCO", "123")
        assertTrue(invalidNewPinResult.isFailure)

        // Correct answer with different casing and whitespace padding
        val newPin = "987654"
        val recoveryResult = securityManager.recoverWithSecurityAnswer("  sAn FrAnCiScO  ", newPin)
        assertTrue(recoveryResult.isSuccess)
        assertTrue(securityManager.isVaultUnlocked.value)

        // Lock and verify new PIN works and old PIN fails
        securityManager.lockVault()
        val oldPinUnlock = securityManager.unlockWithPin(originalPin)
        assertTrue(oldPinUnlock.isFailure)

        val newPinUnlock = securityManager.unlockWithPin(newPin)
        assertTrue(newPinUnlock.isSuccess)
    }

    @Test
    fun testChangePin_requiresOldPinVerification() = runBlocking {
        val pin = "123456"
        val newPin = "789012"
        securityManager.setupInitialPin(pin, "Question", "Answer")

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
        securityManager.setupInitialPin(pin, "Old Question", "Old Answer")

        val newQuestion = "What was your childhood nickname?"
        val newAnswer = "Sparky"
        val changeResult = securityManager.changeRecoveryQuestion(pin, newQuestion, newAnswer)
        assertTrue(changeResult.isSuccess)
        assertEquals(newQuestion, securityManager.getRecoveryQuestion())

        // Verify recovery works with new answer
        securityManager.lockVault()
        val recoveryResult = securityManager.recoverWithSecurityAnswer("sparky", "111222")
        assertTrue(recoveryResult.isSuccess)
    }

    @Test
    fun testQuantumProofPayloadEncryptionAndDecryption() = runBlocking {
        val pin = "123987"
        securityManager.setupInitialPin(pin, "Question", "Answer")

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
}
