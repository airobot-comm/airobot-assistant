package com.airobot.features.aiserv.notepad.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

val Context.aiNotepadDataStore: DataStore<Preferences> by preferencesDataStore(name = "airobot_notepad_prefs")

/**
 * Repository interface for managing AI Notepad persisted activity records.
 */
interface AiNotepadRepository {
    val alarmHistoryFlow: Flow<List<AlarmRecord>>
    val timerHistoryFlow: Flow<List<TimerRecord>>
    val focusHistoryFlow: Flow<List<FocusRecord>>
    val podcastHistoryFlow: Flow<List<PodcastActivityRecord>>

    suspend fun loadAlarmHistory(): List<AlarmRecord>
    suspend fun saveAlarmHistory(list: List<AlarmRecord>)
    suspend fun loadTimerHistory(): List<TimerRecord>
    suspend fun saveTimerHistory(list: List<TimerRecord>)
    suspend fun loadFocusHistory(): List<FocusRecord>
    suspend fun saveFocusHistory(list: List<FocusRecord>)
    suspend fun loadPodcastHistory(): List<PodcastActivityRecord>
    suspend fun savePodcastHistory(list: List<PodcastActivityRecord>)
}

/**
 * Concrete implementation of AiNotepadRepository utilizing DataStore and kotlinx.serialization.
 */
@Singleton
class AiNotepadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AiNotepadRepository {

    private val dataStore = context.aiNotepadDataStore
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "AiNotepadRepository"
        val KEY_ALARM_HISTORY = stringPreferencesKey("alarm_history")
        val KEY_TIMER_HISTORY = stringPreferencesKey("timer_history")
        val KEY_FOCUS_HISTORY = stringPreferencesKey("focus_history")
        val KEY_PODCAST_HISTORY = stringPreferencesKey("podcast_history")
    }

    override val alarmHistoryFlow: Flow<List<AlarmRecord>> = dataStore.data.map { prefs ->
        loadListFromPrefs(prefs, KEY_ALARM_HISTORY)
    }

    override val timerHistoryFlow: Flow<List<TimerRecord>> = dataStore.data.map { prefs ->
        loadListFromPrefs(prefs, KEY_TIMER_HISTORY)
    }

    override val focusHistoryFlow: Flow<List<FocusRecord>> = dataStore.data.map { prefs ->
        loadListFromPrefs(prefs, KEY_FOCUS_HISTORY)
    }

    override val podcastHistoryFlow: Flow<List<PodcastActivityRecord>> = dataStore.data.map { prefs ->
        loadListFromPrefs(prefs, KEY_PODCAST_HISTORY)
    }

    private inline fun <reified T> loadListFromPrefs(prefs: Preferences, key: Preferences.Key<String>): List<T> {
        val jsonString = prefs[key]
        return if (jsonString.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                json.decodeFromString<List<T>>(jsonString)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode notepad list for key ${key.name}", e)
                emptyList()
            }
        }
    }

    private suspend inline fun <reified T> loadList(key: Preferences.Key<String>): List<T> {
        val jsonString = dataStore.data.map { it[key] }.first()
        return if (jsonString.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                json.decodeFromString<List<T>>(jsonString)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode notepad list for key ${key.name}", e)
                emptyList()
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
            Log.e(TAG, "Failed to save notepad list for key ${key.name}", e)
        }
    }

    override suspend fun loadAlarmHistory(): List<AlarmRecord> = loadList(KEY_ALARM_HISTORY)
    override suspend fun saveAlarmHistory(list: List<AlarmRecord>) =
        saveList(KEY_ALARM_HISTORY, list)

    override suspend fun loadTimerHistory(): List<TimerRecord> = loadList(KEY_TIMER_HISTORY)
    override suspend fun saveTimerHistory(list: List<TimerRecord>) =
        saveList(KEY_TIMER_HISTORY, list)

    override suspend fun loadFocusHistory(): List<FocusRecord> = loadList(KEY_FOCUS_HISTORY)
    override suspend fun saveFocusHistory(list: List<FocusRecord>) =
        saveList(KEY_FOCUS_HISTORY, list)

    override suspend fun loadPodcastHistory(): List<PodcastActivityRecord> =
        loadList(KEY_PODCAST_HISTORY)

    override suspend fun savePodcastHistory(list: List<PodcastActivityRecord>) =
        saveList(KEY_PODCAST_HISTORY, list)
}
