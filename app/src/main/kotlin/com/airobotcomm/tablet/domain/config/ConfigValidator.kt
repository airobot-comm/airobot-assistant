package com.airobotcomm.tablet.domain.config

import android.content.Context
import com.airobotcomm.tablet.domain.repository.ConfigRepository
import kotlinx.coroutines.runBlocking

/**
 * 配置验证工具类
 */
class ConfigValidator(private val configRepository: ConfigRepository) {
    private val configManager = ConfigManager(configRepository)

    /**
     * 检查是否需要跳转到设置页面
     * @return true 如果需要跳转到设置页面，false 如果配置完整可以继续
     */
    fun shouldNavigateToSettings(): Boolean {
        return runBlocking {
            val config = configManager.loadConfig()
            config.otaUrl.isBlank() || config.websocketUrl.isBlank()
        }
    }

    /**
     * 检查配置是否完整
     */
    fun isConfigComplete(): Boolean {
        return runBlocking {
            val config = configManager.loadConfig()
            configManager.isConfigComplete(config)
        }
    }

    /**
     * 获取当前配置
     */
    fun getCurrentConfig() = runBlocking { configManager.loadConfig() }
}