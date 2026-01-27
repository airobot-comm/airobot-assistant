package com.airobotcomm.tablet.domain.repository

import com.airobotcomm.tablet.domain.model.DeviceConfig

/**
 * 配置仓库接口 - 定义配置管理的契约
 */
interface OtaConfigRepo {
    /**
     * 保存配置
     */
    suspend fun saveConfig(config: DeviceConfig)
    
    /**
     * 加载配置
     */
    suspend fun loadConfig(): DeviceConfig
    
    /**
     * 检查配置是否完整
     */
    fun isConfigComplete(config: DeviceConfig): Boolean
    
    /**
     * 获取缺失的配置项
     */
    fun getMissingFields(config: DeviceConfig): List<String>
}