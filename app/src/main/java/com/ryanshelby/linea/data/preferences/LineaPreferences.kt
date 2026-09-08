package com.ryanshelby.linea.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "linea_settings")

@Singleton
class LineaPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
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
        val KEY_AUTO_RECORD_CALLS = booleanPreferencesKey("auto_record_calls")
        val KEY_AUTO_RECORD_CONTACTS_ONLY = booleanPreferencesKey("auto_record_contacts_only")
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
        val KEY_OPT_IN_LOCATION_TAG = booleanPreferencesKey("opt_in_location_tag")
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
    val autoRecordCalls: Flow<Boolean> = dataStore.data.map { it[KEY_AUTO_RECORD_CALLS] ?: false }
    val autoRecordContactsOnly: Flow<Boolean> = dataStore.data.map { it[KEY_AUTO_RECORD_CONTACTS_ONLY] ?: false }
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
    val privateModePin: Flow<String> = dataStore.data.map { it[KEY_PRIVATE_MODE_PIN] ?: "1234" }
    val historyViewMode: Flow<String> = dataStore.data.map { it[KEY_HISTORY_VIEW_MODE] ?: "FEED" }
    val callBlockingEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_CALL_BLOCKING_ENABLED] ?: false }
    val optInLocationTag: Flow<Boolean> = dataStore.data.map { it[KEY_OPT_IN_LOCATION_TAG] ?: false }

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

    suspend fun setAutoRecordCalls(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_RECORD_CALLS] = enabled }
    }

    suspend fun setAutoRecordContactsOnly(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_RECORD_CONTACTS_ONLY] = enabled }
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

    suspend fun setPrivateModeUnlocked(unlocked: Boolean) {
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

    suspend fun setOptInLocationTag(enabled: Boolean) {
        dataStore.edit { it[KEY_OPT_IN_LOCATION_TAG] = enabled }
    }
}
