package com.airobotcomm.tablet.domain

import com.airobotcomm.tablet.domain.model.DeviceConfig
import com.airobotcomm.tablet.domain.repository.SysConfigRepo
import javax.inject.Inject

/**
 * 配置业务逻辑类 - 处理配置相关的业务规则
 */
class SystemConfig @Inject constructor(
    private val sysConfigRepo: SysConfigRepo
) {
    /**
     * 保存配置
     */
    suspend fun saveConfig(config: DeviceConfig) = sysConfigRepo.saveConfig(config)

    /**
     * 加载配置
     */
    suspend fun loadConfig(): DeviceConfig = sysConfigRepo.loadConfig()

    /**
     * 检查配置是否完整
     */
    fun isConfigComplete(config: DeviceConfig): Boolean = sysConfigRepo.isConfigComplete(config)

    /**
     * 获取缺失的配置项
     */
    fun getMissingFields(config: DeviceConfig): List<String> = sysConfigRepo.getMissingFields(config)
}