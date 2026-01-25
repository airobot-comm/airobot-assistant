package com.airobotcomm.tablet.domain.config

import com.airobotcomm.tablet.domain.model.DeviceConfig
import com.airobotcomm.tablet.domain.repository.ConfigRepository
import javax.inject.Inject

/**
 * 配置业务逻辑类 - 处理配置相关的业务规则
 */
class ConfigManager @Inject constructor(
    private val configRepository: ConfigRepository
) {
    /**
     * 保存配置
     */
    suspend fun saveConfig(config: DeviceConfig) = configRepository.saveConfig(config)
    
    /**
     * 加载配置
     */
    suspend fun loadConfig(): DeviceConfig = configRepository.loadConfig()
    
    /**
     * 检查配置是否完整
     */
    fun isConfigComplete(config: DeviceConfig): Boolean = configRepository.isConfigComplete(config)
    
    /**
     * 获取缺失的配置项
     */
    fun getMissingFields(config: DeviceConfig): List<String> = configRepository.getMissingFields(config)
}