package com.airobot.features.podcast.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.airobot.features.podcast.data.model.PodcastEpisode
import com.airobot.features.podcast.data.model.PodcastSubscription
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

val Context.podcastDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "airobot_podcast_prefs"
)

/**
 * Repository interface for podcast data persistence.
 *
 * Manages episodes, subscriptions, and playback state via DataStore.
 * System presets are seeded on first launch from [PodcastSystemPresets].
 */
interface PodcastRepository {
    /** Load all episodes (system + DIY), seeding presets on first launch. */
    suspend fun loadEpisodes(): List<PodcastEpisode>

    /** Persist the full episodes list. */
    suspend fun saveEpisodes(list: List<PodcastEpisode>)

    /** Add a single episode and persist. */
    suspend fun addEpisode(episode: PodcastEpisode)

    /** Update playback progress for an episode. */
    suspend fun updateEpisodeProgress(
        id: String,
        lastPositionMs: Long,
        progress: Float,
        played: Boolean
    )

    /** Load all subscriptions, seeding presets on first launch. */
    suspend fun loadSubscriptions(): List<PodcastSubscription>

    /** Persist the full subscriptions list. */
    suspend fun saveSubscriptions(list: List<PodcastSubscription>)

    /** Add a single subscription and persist. */
    suspend fun addSubscription(subscription: PodcastSubscription)
}

/**
 * DataStore-backed implementation of [PodcastRepository].
 *
 * Follows the same pattern as [com.airobot.features.clock.data.ClockRepositoryImpl]:
 * interface + Impl + kotlinx.serialization JSON + DataStore Preferences.
 */
@Singleton
class PodcastRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PodcastRepository {

    private val dataStore = context.podcastDataStore
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "PodcastRepository"
        val KEY_EPISODES = stringPreferencesKey("episodes")
        val KEY_SUBSCRIPTIONS = stringPreferencesKey("subscriptions")
        val KEY_SEEDED = booleanPreferencesKey("system_presets_seeded")
    }

    // ---- Episodes ----

    override suspend fun loadEpisodes(): List<PodcastEpisode> {
        ensureSeeded()
        return loadList(KEY_EPISODES) { emptyList() }
    }

    override suspend fun saveEpisodes(list: List<PodcastEpisode>) {
        saveList(KEY_EPISODES, list)
    }

    override suspend fun addEpisode(episode: PodcastEpisode) {
        val current = loadList<PodcastEpisode>(KEY_EPISODES) { emptyList() }
        saveList(KEY_EPISODES, listOf(episode) + current)
    }

    override suspend fun updateEpisodeProgress(
        id: String,
        lastPositionMs: Long,
        progress: Float,
        played: Boolean
    ) {
        val current = loadList<PodcastEpisode>(KEY_EPISODES) { emptyList() }
        val updated = current.map { ep ->
            if (ep.id == id) {
                ep.copy(
                    lastPositionMs = lastPositionMs,
                    progress = progress,
                    played = played
                )
            } else ep
        }
        saveList(KEY_EPISODES, updated)
    }

    // ---- Subscriptions ----

    override suspend fun loadSubscriptions(): List<PodcastSubscription> {
        ensureSeeded()
        return loadList(KEY_SUBSCRIPTIONS) { emptyList() }
    }

    override suspend fun saveSubscriptions(list: List<PodcastSubscription>) {
        saveList(KEY_SUBSCRIPTIONS, list)
    }

    override suspend fun addSubscription(subscription: PodcastSubscription) {
        val current = loadList<PodcastSubscription>(KEY_SUBSCRIPTIONS) { emptyList() }
        saveList(KEY_SUBSCRIPTIONS, listOf(subscription) + current)
    }

    // ---- Seed System Presets ----

    /**
     * Seed system preset data on first launch.
     * Checks [KEY_SEEDED] flag to avoid re-seeding.
     */
    private suspend fun ensureSeeded() {
        val seeded = dataStore.data.map { it[KEY_SEEDED] ?: false }.first()
        if (seeded) return

        Log.d(TAG, "First launch: seeding system preset data")
        saveList(KEY_EPISODES, PodcastSystemPresets.EPISODES)
        saveList(KEY_SUBSCRIPTIONS, PodcastSystemPresets.SUBSCRIPTIONS)
        dataStore.edit { prefs ->
            prefs[KEY_SEEDED] = true
        }
        Log.d(TAG, "System presets seeded: ${PodcastSystemPresets.EPISODES.size} episodes, " +
            "${PodcastSystemPresets.SUBSCRIPTIONS.size} subscriptions")
    }

    // ---- Generic DataStore Helpers (same pattern as ClockRepositoryImpl) ----

    private suspend inline fun <reified T> loadList(
        key: Preferences.Key<String>,
        defaultProvider: () -> List<T>
    ): List<T> {
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

    private suspend inline fun <reified T> saveList(
        key: Preferences.Key<String>,
        list: List<T>
    ) {
        try {
            val jsonString = json.encodeToString(list)
            dataStore.edit { prefs ->
                prefs[key] = jsonString
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save list for key ${key.name}", e)
        }
    }
}
