package com.airobotcomm.tablet.domain

import com.airobotcomm.tablet.domain.model.DeviceInfo
import com.airobotcomm.tablet.domain.repository.SysConfigRepo
import javax.inject.Inject

/**
 * 配置业务逻辑类 - 处理配置相关的业务规则
 */
class SystemManager @Inject constructor(
    private val sysConfigRepo: SysConfigRepo
) {
    /**
     * 保存配置
     */
    suspend fun saveConfig(config: DeviceInfo) = sysConfigRepo.saveConfig(config)

    /**
     * 加载配置
     */
    suspend fun loadConfig(): DeviceInfo = sysConfigRepo.loadConfig()
}