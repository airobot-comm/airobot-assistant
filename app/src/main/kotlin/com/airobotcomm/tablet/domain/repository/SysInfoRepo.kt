package com.airobotcomm.tablet.domain.repository

import com.airobotcomm.tablet.domain.model.SystemConfig

/**
 * 配置仓库接口 - 定义系统配置管理的契约
 */
interface SysInfoRepo {
    /**
     * 保存系统配置
     */
    suspend fun saveConfig(config: SystemConfig)
    
    /**
     * 加载系统配置
     */
    suspend fun loadConfig(): SystemConfig
}