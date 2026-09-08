package com.ryanshelby.linea.security

import kotlinx.coroutines.flow.Flow

/**
 * Contract for persisting and retrieving cryptographic credentials and state for the Private Safe.
 */
interface VaultPreferences {
    val isVaultPinSet: Flow<Boolean>
    val vaultPinHash: Flow<String?>
    val vaultPinSalt: Flow<String?>
    val vaultMasterKeyEncrypted: Flow<String?>
    val vaultRecoveryQuestion: Flow<String?>
    val vaultRecoverySalt: Flow<String?>
    val vaultRecoveryHash: Flow<String?>
    val vaultRecoveryMasterKeyEncrypted: Flow<String?>
    val vaultFailedAttempts: Flow<Int>
    val vaultLockoutUntil: Flow<Long>

    suspend fun saveVaultConfig(
        pinHash: String,
        pinSalt: String,
        masterKeyEncrypted: String,
        recoveryQuestion: String,
        recoverySalt: String,
        recoveryHash: String,
        recoveryMasterKeyEncrypted: String
    )

    suspend fun updatePinCredentials(
        pinHash: String,
        pinSalt: String,
        masterKeyEncrypted: String
    )

    suspend fun updateRecoveryCredentials(
        question: String,
        salt: String,
        hash: String,
        masterKeyEnc: String
    )

    suspend fun recordFailedVaultAttempt(failedAttempts: Int, lockoutUntil: Long)

    suspend fun resetVaultLockout()

    suspend fun setPrivateModeUnlocked(unlocked: Boolean)
}
