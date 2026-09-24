package com.airobot.features.clock.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.airobot.features.clock.data.model.AlarmItem
import com.airobot.features.clock.data.model.PresetItem
import com.airobot.features.clock.data.model.HourlyChimeConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

val Context.clockDataStore: DataStore<Preferences> by preferencesDataStore(name = "airobot_clock_prefs")

/**
 * Repository interface for managing clock-related persisted data.
 */
interface ClockRepository {
    suspend fun loadAlarms(): List<AlarmItem>
    suspend fun saveAlarms(list: List<AlarmItem>)
    suspend fun loadTimerPresets(): List<PresetItem>
    suspend fun saveTimerPresets(list: List<PresetItem>)
    suspend fun loadHourlyChimeEnabled(): Boolean
    suspend fun saveHourlyChimeEnabled(enabled: Boolean)
    suspend fun loadHourlyChimeConfig(): HourlyChimeConfig
    suspend fun saveHourlyChimeConfig(config: HourlyChimeConfig)
}

/**
 * implementation of ClockRepository utilizing DataStore and kotlinx.serialization.
 */
@Singleton
class ClockRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ClockRepository {

    private val dataStore = context.clockDataStore
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "ClockRepository"
        val KEY_ALARMS = stringPreferencesKey("alarms")
        val KEY_TIMER_PRESETS = stringPreferencesKey("timer_presets")
        val KEY_HOURLY_CHIME = booleanPreferencesKey("hourly_chime")
        val KEY_HOURLY_CHIME_CONFIG = stringPreferencesKey("hourly_chime_config")
    }

    private suspend inline fun <reified T> loadList(key: Preferences.Key<String>, defaultProvider: () -> List<T>): List<T> {
        val jsonString = dataStore.data.map { it[key] }.first()
        return if (jsonString.isNullOrEmpty()) {
            defaultProvider()
        } else {
            try {
                json.decodeFromString<List<T>>(jsonString)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode list for key ${key.name}", e)
                defaultProvider()
            }
        }
    }

    private suspend inline fun <reified T> saveList(key: Preferences.Key<String>, list: List<T>) {
        try {
            val jsonString = json.encodeToString(list)
            dataStore.edit { prefs ->
                prefs[key] = jsonString
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save list for key ${key.name}", e)
        }
    }

    override suspend fun loadAlarms(): List<AlarmItem> = loadList(KEY_ALARMS) { emptyList() }
    override suspend fun saveAlarms(list: List<AlarmItem>) = saveList(KEY_ALARMS, list)

    override suspend fun loadTimerPresets(): List<PresetItem> = loadList(KEY_TIMER_PRESETS) { emptyList() }
    override suspend fun saveTimerPresets(list: List<PresetItem>) = saveList(KEY_TIMER_PRESETS, list)

    override suspend fun loadHourlyChimeEnabled(): Boolean {
        return loadHourlyChimeConfig().enabled
    }

    override suspend fun saveHourlyChimeEnabled(enabled: Boolean) {
        val config = loadHourlyChimeConfig()
        saveHourlyChimeConfig(config.copy(enabled = enabled))
    }

    override suspend fun loadHourlyChimeConfig(): HourlyChimeConfig {
        val jsonString = dataStore.data.map { it[KEY_HOURLY_CHIME_CONFIG] }.first()
        return if (jsonString.isNullOrEmpty()) {
            // Check legacy enabled boolean for backward compatibility
            val legacyEnabled = dataStore.data.map { it[KEY_HOURLY_CHIME] ?: true }.first()
            HourlyChimeConfig(enabled = legacyEnabled)
        } else {
            try {
                json.decodeFromString<HourlyChimeConfig>(jsonString)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode HourlyChimeConfig", e)
                HourlyChimeConfig()
            }
        }
    }

    override suspend fun saveHourlyChimeConfig(config: HourlyChimeConfig) {
        try {
            val jsonString = json.encodeToString(config)
            dataStore.edit { prefs ->
                prefs[KEY_HOURLY_CHIME_CONFIG] = jsonString
                prefs[KEY_HOURLY_CHIME] = config.enabled
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save HourlyChimeConfig", e)
        }
    }
}
