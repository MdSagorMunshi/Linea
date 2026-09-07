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
        val KEY_COMPACT_DIALPAD = booleanPreferencesKey("compact_dialpad")
        val KEY_CLEANUP_DAYS = intPreferencesKey("cleanup_days") // 0, 30, 90, 180
        val KEY_BLOCK_UNKNOWN = booleanPreferencesKey("block_unknown")
        val KEY_BLOCK_PRIVATE = booleanPreferencesKey("block_private")
        val KEY_BLOCK_NON_CONTACTS = booleanPreferencesKey("block_non_contacts")
        val KEY_BLOCK_INTERNATIONAL = booleanPreferencesKey("block_international")
        val KEY_ALLOW_LIST_MODE = booleanPreferencesKey("allow_list_mode")
    }

    val reduceAnimations: Flow<Boolean> = dataStore.data.map { it[KEY_REDUCE_ANIMATIONS] ?: false }
    val defaultSim: Flow<Int> = dataStore.data.map { it[KEY_DEFAULT_SIM] ?: 0 }
    val askSimBeforeDial: Flow<Boolean> = dataStore.data.map { it[KEY_ASK_SIM_BEFORE_DIAL] ?: false }
    val proximitySensorEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_PROXIMITY_SENSOR] ?: true }
    val dialpadSoundEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_DIALPAD_SOUND] ?: true }
    val dialpadVibrationEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_DIALPAD_VIBRATION] ?: true }
    val callVibrationEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_CALL_VIBRATION] ?: true }
    val callCountdownSeconds: Flow<Int> = dataStore.data.map { it[KEY_CALL_COUNTDOWN_SECONDS] ?: 0 }
    val repeatCallOverride: Flow<Boolean> = dataStore.data.map { it[KEY_REPEAT_CALL_OVERRIDE] ?: true }

    suspend fun setReduceAnimations(enabled: Boolean) {
        dataStore.edit { it[KEY_REDUCE_ANIMATIONS] = enabled }
    }

    suspend fun setDefaultSim(sim: Int) {
        dataStore.edit { it[KEY_DEFAULT_SIM] = sim }
    }

    suspend fun setProximitySensor(enabled: Boolean) {
        dataStore.edit { it[KEY_PROXIMITY_SENSOR] = enabled }
    }

    suspend fun setCountdownSeconds(seconds: Int) {
        dataStore.edit { it[KEY_CALL_COUNTDOWN_SECONDS] = seconds }
    }
}
