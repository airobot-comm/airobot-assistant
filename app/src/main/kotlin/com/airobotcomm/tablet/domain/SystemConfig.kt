package com.airobotcomm.tablet.domain

import com.airobotcomm.tablet.domain.model.DeviceConfig
import com.airobotcomm.tablet.domain.repository.OtaConfigRepo
import javax.inject.Inject

/**
 * 配置业务逻辑类 - 处理配置相关的业务规则
 */
class SystemConfig @Inject constructor(
    private val otaConfigRepo: OtaConfigRepo
) {
    /**
     * 保存配置
     */
    suspend fun saveConfig(config: DeviceConfig) = otaConfigRepo.saveConfig(config)

    /**
     * 加载配置
     */
    suspend fun loadConfig(): DeviceConfig = otaConfigRepo.loadConfig()

    /**
     * 检查配置是否完整
     */
    fun isConfigComplete(config: DeviceConfig): Boolean = otaConfigRepo.isConfigComplete(config)

    /**
     * 获取缺失的配置项
     */
    fun getMissingFields(config: DeviceConfig): List<String> = otaConfigRepo.getMissingFields(config)
}