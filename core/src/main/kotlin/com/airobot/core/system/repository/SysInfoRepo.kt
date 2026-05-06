package com.airobot.core.system.repository

import com.airobot.core.system.model.SystemInfo

/**
 * 配置仓库接口 - 定义系统配置管理的契约
 */
interface SysInfoRepo {
    /**
     * 保存系统配置
     */
    /**
     * 保存系统配置
     */
    suspend fun saveConfig(config: SystemInfo)
    
    /**
     * 加载系统配置
     */
    suspend fun loadConfig(): SystemInfo
}

