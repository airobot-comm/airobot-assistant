package com.airobotcomm.tablet.infra.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.airobotcomm.tablet.domain.model.DeviceConfig
import com.airobotcomm.tablet.domain.repository.SysConfigRepo
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "device_config")

@Singleton
class SysConfigRepoImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SysConfigRepo {
    
    private val gson = Gson()
    
    private object PreferencesKeys {
        val CONFIG_DATA = stringPreferencesKey("config_data")
    }

    override suspend fun saveConfig(config: DeviceConfig) {
        withContext(Dispatchers.IO) {
            context.dataStore.edit { preferences ->
                val jsonString = gson.toJson(config)
                preferences[PreferencesKeys.CONFIG_DATA] = jsonString
            }
        }
    }

    override suspend fun loadConfig(): DeviceConfig {
        return withContext(Dispatchers.IO) {
            val preferences = context.dataStore.data.first()
            val configJson = preferences[PreferencesKeys.CONFIG_DATA]
            
            val config = if (configJson != null) {
                try {
                    gson.fromJson(configJson, DeviceConfig::class.java)
                } catch (e: Exception) {
                    DeviceConfig.createDefault()
                }
            } else {
                DeviceConfig.createDefault()
            }
            
            // 确保字段不为 null (防御 Gson 反序列化时由于字段缺失导致的 null 注入)
            config.copy(
                deviceModel = (config.deviceModel as String?) ?: "default",
                name = (config.name as String?) ?: "测试",
                otaUrl = (config.otaUrl as String?) ?: "",
                websocketUrl = (config.websocketUrl as String?) ?: "",
                deviceId = (config.deviceId as String?) ?: "",
                clientId = (config.clientId as String?) ?: DeviceConfig.generateClientId(),
                token = (config.token as String?) ?: "test-token",
                activationCode = (config.activationCode as String?) ?: "",
            )
        }
    }

    override fun isConfigComplete(config: DeviceConfig): Boolean {
        return config.name.isNotBlank() &&
               (config.otaUrl.isNotBlank() || config.websocketUrl.isNotBlank()) &&
               config.deviceId.isNotBlank() &&
               config.token.isNotBlank()
    }

    override fun getMissingFields(config: DeviceConfig): List<String> {
        val missingFields = mutableListOf<String>()

        if (config.name.isBlank()) missingFields.add("设备名称")
        if (config.otaUrl.isBlank() && config.websocketUrl.isBlank()) missingFields.add("OTA地址或WSS地址(至少填一个)")
        if (config.deviceId.isBlank()) missingFields.add("MAC地址")
        if (config.token.isBlank()) missingFields.add("Token")

        return missingFields
    }
}
