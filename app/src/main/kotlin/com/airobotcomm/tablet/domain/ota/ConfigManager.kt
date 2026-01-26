package com.airobotcomm.tablet.domain.ota

import com.airobotcomm.tablet.domain.ota.model.DeviceConfig
import com.airobotcomm.tablet.domain.ota.repository.OtaConfigRepository
import javax.inject.Inject

/**
 * 配置业务逻辑类 - 处理配置相关的业务规则
 */
class ConfigManager @Inject constructor(
    private val otaConfigRepository: OtaConfigRepository
) {
    /**
     * 保存配置
     */
    suspend fun saveConfig(config: DeviceConfig) = otaConfigRepository.saveConfig(config)

    /**
     * 加载配置
     */
    suspend fun loadConfig(): DeviceConfig = otaConfigRepository.loadConfig()

    /**
     * 检查配置是否完整
     */
    fun isConfigComplete(config: DeviceConfig): Boolean = otaConfigRepository.isConfigComplete(config)

    /**
     * 获取缺失的配置项
     */
    fun getMissingFields(config: DeviceConfig): List<String> = otaConfigRepository.getMissingFields(config)
}