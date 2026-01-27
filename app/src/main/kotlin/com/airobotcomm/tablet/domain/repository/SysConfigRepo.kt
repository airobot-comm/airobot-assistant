package com.airobotcomm.tablet.domain.repository

import com.airobotcomm.tablet.domain.model.DeviceInfo

/**
 * 配置仓库接口 - 定义配置管理的契约
 */
interface SysConfigRepo {
    /**
     * 保存配置
     */
    suspend fun saveConfig(config: DeviceInfo)
    
    /**
     * 加载配置
     */
    suspend fun loadConfig(): DeviceInfo
    
    /**
     * 检查配置是否完整
     */
    fun isConfigComplete(config: DeviceInfo): Boolean
    
    /**
     * 获取缺失的配置项
     */
    fun getMissingFields(config: DeviceInfo): List<String>
}