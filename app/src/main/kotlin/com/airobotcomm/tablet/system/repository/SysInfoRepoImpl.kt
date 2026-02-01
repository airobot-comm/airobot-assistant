package com.airobotcomm.tablet.system.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.airobotcomm.tablet.system.model.SystemInfo
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "system_config")

@Singleton
class SysInfoRepoImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SysInfoRepo {
    
    private val gson = Gson()
    
    private object PreferencesKeys {
        val CONFIG_DATA = stringPreferencesKey("system_info_config_json")
    }

    override suspend fun saveConfig(config: SystemInfo) {
        withContext(Dispatchers.IO) {
            context.dataStore.edit { preferences ->
                val jsonString = gson.toJson(config)
                preferences[PreferencesKeys.CONFIG_DATA] = jsonString
            }
        }
    }

    override suspend fun loadConfig(): SystemInfo {
        return withContext(Dispatchers.IO) {
            val preferences = context.dataStore.data.first()
            val configJson = preferences[PreferencesKeys.CONFIG_DATA]
            
            if (configJson != null) {
                try {
                    gson.fromJson(configJson, SystemInfo::class.java)
                } catch (e: Exception) {
                    SystemInfo()
                }
            } else {
                SystemInfo()
            }
        }
    }
}
