package com.airobot.airbot.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.airobot.airbot.data.CharacterConfig
import com.airobot.airbot.data.CharacterRepo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.characterDataStore: DataStore<Preferences> by preferencesDataStore(name = "character_config")

@Singleton
class CharacterRepoImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CharacterRepo {

    private val json = Json { ignoreUnknownKeys = true }

    private object PreferencesKeys {
        val CONFIG_DATA = stringPreferencesKey("character_info_config_json")
    }

    override suspend fun saveConfig(config: CharacterConfig) {
        withContext(Dispatchers.IO) {
            context.characterDataStore.edit { preferences ->
                val jsonString = json.encodeToString(config)
                preferences[PreferencesKeys.CONFIG_DATA] = jsonString
            }
        }
    }

    override suspend fun loadConfig(): CharacterConfig {
        return withContext(Dispatchers.IO) {
            val preferences = context.characterDataStore.data.first()
            val configJson = preferences[PreferencesKeys.CONFIG_DATA]

            if (configJson != null) {
                try {
                    json.decodeFromString<CharacterConfig>(configJson)
                } catch (e: Exception) {
                    CharacterConfig()
                }
            } else {
                CharacterConfig()
            }
        }
    }
}
