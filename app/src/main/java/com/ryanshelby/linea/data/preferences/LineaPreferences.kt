package com.ryanshelby.linea.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.ryanshelby.linea.security.VaultPreferences
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "linea_settings")

@Singleton
class LineaPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) : VaultPreferences {
    private val dataStore = context.dataStore

    object RingtoneType {
        const val APP_DEFAULT = "APP_DEFAULT"
        const val SYSTEM_DEFAULT = "SYSTEM_DEFAULT"
    }

    object NavbarMode {
        const val COLLAPSED = "COLLAPSED"
        const val EXPANDED = "EXPANDED"
    }

    object DialpadHapticProfile {
        const val TITANIUM_GLASS = "TITANIUM_GLASS"
        const val MECHANICAL_RELAY = "MECHANICAL_RELAY"
        const val STEALTH = "STEALTH"
        const val CLASSIC = "CLASSIC"
    }

    companion object {
        val KEY_NAVBAR_MODE = stringPreferencesKey("navbar_mode")
        val KEY_DEFAULT_SIM = intPreferencesKey("default_sim") // 0 = SIM 1, 1 = SIM 2, -1 = Ask Every Time
        val KEY_ASK_SIM_BEFORE_DIAL = booleanPreferencesKey("ask_sim_before_dial")
        val KEY_REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
        val KEY_PROXIMITY_SENSOR = booleanPreferencesKey("proximity_sensor")
        val KEY_DIALPAD_SOUND = booleanPreferencesKey("dialpad_sound")
        val KEY_DIALPAD_VIBRATION = booleanPreferencesKey("dialpad_vibration")
        val KEY_CALL_VIBRATION = booleanPreferencesKey("call_vibration")
        val KEY_THEME = stringPreferencesKey("theme") // DARK, LIGHT, SYSTEM, AMOLED
        val KEY_NUMBER_FORMATTING = booleanPreferencesKey("number_formatting")
        val KEY_CALL_CONFIRMATION = booleanPreferencesKey("call_confirmation")
        val KEY_CALL_COUNTDOWN_SECONDS = intPreferencesKey("call_countdown_seconds") // 0, 3, 5, 10
        val KEY_DONT_INTERRUPT_ME = booleanPreferencesKey("dont_interrupt_me")
        val KEY_REPEAT_CALL_OVERRIDE = booleanPreferencesKey("repeat_call_override")
        val KEY_CALL_DURATION_WARNING_MINUTES = intPreferencesKey("call_duration_warning_minutes") // 0 = off, 5, 10, 15, 30
        val KEY_VOICEMAIL_NUMBER = stringPreferencesKey("voicemail_number")
        val KEY_COMPACT_DIALPAD = booleanPreferencesKey("compact_dialpad")
        val KEY_CLEANUP_DAYS = intPreferencesKey("cleanup_days") // 0, 30, 90, 180
        val KEY_BLOCK_UNKNOWN = booleanPreferencesKey("block_unknown")
        val KEY_BLOCK_PRIVATE = booleanPreferencesKey("block_private")
        val KEY_BLOCK_NON_CONTACTS = booleanPreferencesKey("block_non_contacts")
        val KEY_BLOCK_INTERNATIONAL = booleanPreferencesKey("block_international")
        val KEY_ALLOW_LIST_MODE = booleanPreferencesKey("allow_list_mode")
        val KEY_ACTIVE_PROFILE = stringPreferencesKey("active_profile") // PERSONAL, WORK, CUSTOM
        val KEY_PRIVATE_MODE_UNLOCKED = booleanPreferencesKey("private_mode_unlocked")
        val KEY_PRIVATE_MODE_PIN = stringPreferencesKey("private_mode_pin")
        val KEY_HISTORY_VIEW_MODE = stringPreferencesKey("history_view_mode") // FEED, SESSIONS
        val KEY_CALL_BLOCKING_ENABLED = booleanPreferencesKey("call_blocking_enabled")
        val KEY_LAST_CONTACT_ACCOUNT_NAME = stringPreferencesKey("last_contact_account_name")
        val KEY_LAST_CONTACT_ACCOUNT_TYPE = stringPreferencesKey("last_contact_account_type")
        val KEY_OPT_IN_LOCATION_TAG = booleanPreferencesKey("opt_in_location_tag")
        val KEY_RINGTONE_TYPE = stringPreferencesKey("ringtone_type") // "APP_DEFAULT", "SYSTEM_DEFAULT"
        val KEY_FLIP_TO_SILENCE = booleanPreferencesKey("flip_to_silence")
        val KEY_PROXIMITY_WAVE_TO_SILENCE = booleanPreferencesKey("proximity_wave_to_silence")
        val KEY_DIALPAD_HAPTIC_PROFILE = stringPreferencesKey("dialpad_haptic_profile") // TITANIUM_GLASS, MECHANICAL_RELAY, STEALTH, CLASSIC
        val KEY_PERMISSION_INTRO_SHOWN = booleanPreferencesKey("permission_intro_shown")

        // Quantum Vault & Anti-Brute-Force Keys

        val KEY_VAULT_HAS_PIN = booleanPreferencesKey("vault_has_pin")
        val KEY_VAULT_PIN_HASH = stringPreferencesKey("vault_pin_hash")
        val KEY_VAULT_SALT = stringPreferencesKey("vault_salt")
        val KEY_VAULT_MASTER_KEY_ENC = stringPreferencesKey("vault_master_key_enc")
        val KEY_VAULT_RECOVERY_QUESTION = stringPreferencesKey("vault_recovery_question")
        val KEY_VAULT_RECOVERY_SALT = stringPreferencesKey("vault_recovery_salt")
        val KEY_VAULT_RECOVERY_HASH = stringPreferencesKey("vault_recovery_hash")
        val KEY_VAULT_RECOVERY_MASTER_KEY_ENC = stringPreferencesKey("vault_recovery_master_key_enc")
        val KEY_VAULT_FAILED_ATTEMPTS = intPreferencesKey("vault_failed_attempts")
        val KEY_VAULT_LOCKOUT_UNTIL = longPreferencesKey("vault_lockout_until")
    }

    val reduceAnimations: Flow<Boolean> = dataStore.data.map { it[KEY_REDUCE_ANIMATIONS] ?: false }
    val defaultSim: Flow<Int> = dataStore.data.map { it[KEY_DEFAULT_SIM] ?: 0 }
    val askSimBeforeDial: Flow<Boolean> = dataStore.data.map { it[KEY_ASK_SIM_BEFORE_DIAL] ?: false }
    val proximitySensorEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_PROXIMITY_SENSOR] ?: true }
    val dialpadSoundEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_DIALPAD_SOUND] ?: true }
    val dialpadVibrationEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_DIALPAD_VIBRATION] ?: true }
    val callVibrationEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_CALL_VIBRATION] ?: true }
    val callConfirmationEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_CALL_CONFIRMATION] ?: false }
    val callCountdownSeconds: Flow<Int> = dataStore.data.map { it[KEY_CALL_COUNTDOWN_SECONDS] ?: 0 }
    val dontInterruptMe: Flow<Boolean> = dataStore.data.map { it[KEY_DONT_INTERRUPT_ME] ?: false }
    val repeatCallOverride: Flow<Boolean> = dataStore.data.map { it[KEY_REPEAT_CALL_OVERRIDE] ?: true }
    val callDurationWarningMinutes: Flow<Int> = dataStore.data.map { it[KEY_CALL_DURATION_WARNING_MINUTES] ?: 0 }
    val voicemailNumber: Flow<String> = dataStore.data.map { it[KEY_VOICEMAIL_NUMBER] ?: "123" }
    val blockUnknown: Flow<Boolean> = dataStore.data.map { it[KEY_BLOCK_UNKNOWN] ?: false }
    val blockPrivate: Flow<Boolean> = dataStore.data.map { it[KEY_BLOCK_PRIVATE] ?: false }
    val blockNonContacts: Flow<Boolean> = dataStore.data.map { it[KEY_BLOCK_NON_CONTACTS] ?: false }
    val blockInternational: Flow<Boolean> = dataStore.data.map { it[KEY_BLOCK_INTERNATIONAL] ?: false }
    val allowListMode: Flow<Boolean> = dataStore.data.map { it[KEY_ALLOW_LIST_MODE] ?: false }
    val themePreference: Flow<String> = dataStore.data.map { it[KEY_THEME] ?: "DARK" }
    val cleanupDays: Flow<Int> = dataStore.data.map { it[KEY_CLEANUP_DAYS] ?: 0 }
    val compactDialpad: Flow<Boolean> = dataStore.data.map { it[KEY_COMPACT_DIALPAD] ?: false }
    val activeProfile: Flow<String> = dataStore.data.map { it[KEY_ACTIVE_PROFILE] ?: "PERSONAL" }
    val privateModeUnlocked: Flow<Boolean> = dataStore.data.map { it[KEY_PRIVATE_MODE_UNLOCKED] ?: false }
    val privateModePin: Flow<String> = dataStore.data.map { it[KEY_PRIVATE_MODE_PIN] ?: "" }
    val historyViewMode: Flow<String> = dataStore.data.map { it[KEY_HISTORY_VIEW_MODE] ?: "FEED" }
    val callBlockingEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_CALL_BLOCKING_ENABLED] ?: false }
    val lastContactAccountName: Flow<String?> = dataStore.data.map { it[KEY_LAST_CONTACT_ACCOUNT_NAME] }
    val lastContactAccountType: Flow<String?> = dataStore.data.map { it[KEY_LAST_CONTACT_ACCOUNT_TYPE] }
    val optInLocationTag: Flow<Boolean> = dataStore.data.map { it[KEY_OPT_IN_LOCATION_TAG] ?: false }
    val ringtoneType: Flow<String> = dataStore.data.map { it[KEY_RINGTONE_TYPE] ?: RingtoneType.APP_DEFAULT }
    val navbarMode: Flow<String> = dataStore.data.map { it[KEY_NAVBAR_MODE] ?: NavbarMode.COLLAPSED }
    val flipToSilenceEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_FLIP_TO_SILENCE] ?: false }
    val proximityWaveToSilenceEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_PROXIMITY_WAVE_TO_SILENCE] ?: false }
    val dialpadHapticProfile: Flow<String> = dataStore.data.map { it[KEY_DIALPAD_HAPTIC_PROFILE] ?: DialpadHapticProfile.TITANIUM_GLASS }
    val permissionIntroShown: Flow<Boolean> = dataStore.data.map { it[KEY_PERMISSION_INTRO_SHOWN] ?: false }

    // Vault DataStore Flows
    override val isVaultPinSet: Flow<Boolean> = dataStore.data.map { it[KEY_VAULT_HAS_PIN] ?: false }
    override val vaultPinHash: Flow<String?> = dataStore.data.map { it[KEY_VAULT_PIN_HASH] }
    override val vaultPinSalt: Flow<String?> = dataStore.data.map { it[KEY_VAULT_SALT] }
    override val vaultMasterKeyEncrypted: Flow<String?> = dataStore.data.map { it[KEY_VAULT_MASTER_KEY_ENC] }
    override val vaultRecoveryQuestion: Flow<String?> = dataStore.data.map { it[KEY_VAULT_RECOVERY_QUESTION] }
    override val vaultRecoverySalt: Flow<String?> = dataStore.data.map { it[KEY_VAULT_RECOVERY_SALT] }
    override val vaultRecoveryHash: Flow<String?> = dataStore.data.map { it[KEY_VAULT_RECOVERY_HASH] }
    override val vaultRecoveryMasterKeyEncrypted: Flow<String?> = dataStore.data.map { it[KEY_VAULT_RECOVERY_MASTER_KEY_ENC] }
    override val vaultFailedAttempts: Flow<Int> = dataStore.data.map { it[KEY_VAULT_FAILED_ATTEMPTS] ?: 0 }
    override val vaultLockoutUntil: Flow<Long> = dataStore.data.map { it[KEY_VAULT_LOCKOUT_UNTIL] ?: 0L }

    suspend fun setFlipToSilence(enabled: Boolean) {
        dataStore.edit { it[KEY_FLIP_TO_SILENCE] = enabled }
    }

    suspend fun setPermissionIntroShown(shown: Boolean) {
        dataStore.edit { it[KEY_PERMISSION_INTRO_SHOWN] = shown }
    }

    suspend fun setProximityWaveToSilence(enabled: Boolean) {
        dataStore.edit { it[KEY_PROXIMITY_WAVE_TO_SILENCE] = enabled }
    }

    suspend fun setDialpadHapticProfile(profile: String) {
        dataStore.edit { it[KEY_DIALPAD_HAPTIC_PROFILE] = profile }
    }

    suspend fun setReduceAnimations(enabled: Boolean) {
        dataStore.edit { it[KEY_REDUCE_ANIMATIONS] = enabled }
    }

    suspend fun setDefaultSim(sim: Int) {
        dataStore.edit { it[KEY_DEFAULT_SIM] = sim }
    }

    suspend fun setAskSimBeforeDial(enabled: Boolean) {
        dataStore.edit { it[KEY_ASK_SIM_BEFORE_DIAL] = enabled }
    }

    suspend fun setProximitySensor(enabled: Boolean) {
        dataStore.edit { it[KEY_PROXIMITY_SENSOR] = enabled }
    }

    suspend fun setDialpadSound(enabled: Boolean) {
        dataStore.edit { it[KEY_DIALPAD_SOUND] = enabled }
    }

    suspend fun setDialpadVibration(enabled: Boolean) {
        dataStore.edit { it[KEY_DIALPAD_VIBRATION] = enabled }
    }

    suspend fun setCallVibration(enabled: Boolean) {
        dataStore.edit { it[KEY_CALL_VIBRATION] = enabled }
    }

    suspend fun setCallConfirmation(enabled: Boolean) {
        dataStore.edit { it[KEY_CALL_CONFIRMATION] = enabled }
    }

    suspend fun setCountdownSeconds(seconds: Int) {
        dataStore.edit { it[KEY_CALL_COUNTDOWN_SECONDS] = seconds }
    }

    suspend fun setRepeatCallOverride(enabled: Boolean) {
        dataStore.edit { it[KEY_REPEAT_CALL_OVERRIDE] = enabled }
    }

    suspend fun setDontInterruptMe(enabled: Boolean) {
        dataStore.edit { it[KEY_DONT_INTERRUPT_ME] = enabled }
    }

    suspend fun setBlockUnknown(enabled: Boolean) {
        dataStore.edit { it[KEY_BLOCK_UNKNOWN] = enabled }
    }

    suspend fun setBlockPrivate(enabled: Boolean) {
        dataStore.edit { it[KEY_BLOCK_PRIVATE] = enabled }
    }

    suspend fun setBlockNonContacts(enabled: Boolean) {
        dataStore.edit { it[KEY_BLOCK_NON_CONTACTS] = enabled }
    }

    suspend fun setBlockInternational(enabled: Boolean) {
        dataStore.edit { it[KEY_BLOCK_INTERNATIONAL] = enabled }
    }

    suspend fun setAllowListMode(enabled: Boolean) {
        dataStore.edit { it[KEY_ALLOW_LIST_MODE] = enabled }
    }

    suspend fun setThemePreference(theme: String) {
        dataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun setCallDurationWarningMinutes(minutes: Int) {
        dataStore.edit { it[KEY_CALL_DURATION_WARNING_MINUTES] = minutes }
    }

    suspend fun setVoicemailNumber(number: String) {
        dataStore.edit { it[KEY_VOICEMAIL_NUMBER] = number }
    }

    suspend fun setCleanupDays(days: Int) {
        dataStore.edit { it[KEY_CLEANUP_DAYS] = days }
    }

    suspend fun setCompactDialpad(enabled: Boolean) {
        dataStore.edit { it[KEY_COMPACT_DIALPAD] = enabled }
    }

    suspend fun setActiveProfile(profile: String) {
        dataStore.edit { it[KEY_ACTIVE_PROFILE] = profile }
    }

    override suspend fun setPrivateModeUnlocked(unlocked: Boolean) {
        dataStore.edit { it[KEY_PRIVATE_MODE_UNLOCKED] = unlocked }
    }

    suspend fun setPrivateModePin(pin: String) {
        dataStore.edit { it[KEY_PRIVATE_MODE_PIN] = pin }
    }

    suspend fun setHistoryViewMode(mode: String) {
        dataStore.edit { it[KEY_HISTORY_VIEW_MODE] = mode }
    }

    suspend fun setCallBlockingEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_CALL_BLOCKING_ENABLED] = enabled }
    }

    suspend fun setLastContactAccount(name: String?, type: String?) {
        dataStore.edit {
            if (name != null) it[KEY_LAST_CONTACT_ACCOUNT_NAME] = name else it.remove(KEY_LAST_CONTACT_ACCOUNT_NAME)
            if (type != null) it[KEY_LAST_CONTACT_ACCOUNT_TYPE] = type else it.remove(KEY_LAST_CONTACT_ACCOUNT_TYPE)
        }
    }

    suspend fun setOptInLocationTag(enabled: Boolean) {
        dataStore.edit { it[KEY_OPT_IN_LOCATION_TAG] = enabled }
    }



    suspend fun setRingtoneType(type: String) {
        dataStore.edit { it[KEY_RINGTONE_TYPE] = type }
    }

    suspend fun setNavbarMode(mode: String) {
        dataStore.edit { it[KEY_NAVBAR_MODE] = mode }
    }

    override suspend fun saveVaultConfig(
        pinHash: String,
        pinSalt: String,
        masterKeyEncrypted: String,
        recoveryQuestion: String,
        recoverySalt: String,
        recoveryHash: String,
        recoveryMasterKeyEncrypted: String
    ) {
        dataStore.edit {
            it[KEY_VAULT_HAS_PIN] = true
            it[KEY_VAULT_PIN_HASH] = pinHash
            it[KEY_VAULT_SALT] = pinSalt
            it[KEY_VAULT_MASTER_KEY_ENC] = masterKeyEncrypted
            it[KEY_VAULT_RECOVERY_QUESTION] = recoveryQuestion
            it[KEY_VAULT_RECOVERY_SALT] = recoverySalt
            it[KEY_VAULT_RECOVERY_HASH] = recoveryHash
            it[KEY_VAULT_RECOVERY_MASTER_KEY_ENC] = recoveryMasterKeyEncrypted
            it[KEY_VAULT_FAILED_ATTEMPTS] = 0
            it[KEY_VAULT_LOCKOUT_UNTIL] = 0L
        }
    }

    override suspend fun updatePinCredentials(
        pinHash: String,
        pinSalt: String,
        masterKeyEncrypted: String
    ) {
        dataStore.edit {
            it[KEY_VAULT_HAS_PIN] = true
            it[KEY_VAULT_PIN_HASH] = pinHash
            it[KEY_VAULT_SALT] = pinSalt
            it[KEY_VAULT_MASTER_KEY_ENC] = masterKeyEncrypted
            it[KEY_VAULT_FAILED_ATTEMPTS] = 0
            it[KEY_VAULT_LOCKOUT_UNTIL] = 0L
        }
    }

    override suspend fun updateRecoveryCredentials(
        question: String,
        salt: String,
        hash: String,
        masterKeyEnc: String
    ) {
        dataStore.edit {
            it[KEY_VAULT_RECOVERY_QUESTION] = question
            it[KEY_VAULT_RECOVERY_SALT] = salt
            it[KEY_VAULT_RECOVERY_HASH] = hash
            it[KEY_VAULT_RECOVERY_MASTER_KEY_ENC] = masterKeyEnc
        }
    }

    override suspend fun recordFailedVaultAttempt(failedAttempts: Int, lockoutUntil: Long) {
        dataStore.edit {
            it[KEY_VAULT_FAILED_ATTEMPTS] = failedAttempts
            it[KEY_VAULT_LOCKOUT_UNTIL] = lockoutUntil
        }
    }

    override suspend fun resetVaultLockout() {
        dataStore.edit {
            it[KEY_VAULT_FAILED_ATTEMPTS] = 0
            it[KEY_VAULT_LOCKOUT_UNTIL] = 0L
        }
    }
}
