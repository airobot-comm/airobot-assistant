package com.airobotcomm.tablet.system

import android.content.Context
import com.airobotcomm.tablet.system.model.DeviceInfo
import com.airobotcomm.tablet.system.model.SystemInfo
import com.airobotcomm.tablet.system.model.ActiveInfo
import com.airobotcomm.tablet.system.repository.SysInfoRepo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 配置业务逻辑类 - 处理基础设备信息展示与系统配置管理的业务规则
 * 遵循封闭原则，不对外直接暴露内部存储实现与模型细节
 */
@Singleton
class SysManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sysInfoRepo: SysInfoRepo
) {
    private val mutex = Mutex()
    // Cache the system info in memory
    private var _systemInfo: SystemInfo? = null

    /**
     * 系统启动时调用的初始化方法，获取不可修改的设备基础信息
     */
    private suspend fun ensureSystemInfo(): SystemInfo {
        if (_systemInfo == null) {
            var sInfo = sysInfoRepo.loadConfig()
            
            // Check if initialization is needed (empty device Id)
            if (sInfo.deviceInfo!!.deviceId.isEmpty()) {
                 val deviceInfo = DeviceInfo.create(context)
                 
                 // Re-construct SystemInfo with initialized data
                 sInfo = sInfo.copy(
                     deviceInfo = deviceInfo,
                     // Ensure active info is also present if default was empty
                     activeInfo = if(sInfo.activeInfo!!.productKey.isEmpty())
                         ActiveInfo(productKey="", secretKey="", serviceTime="") else sInfo.activeInfo
                 )
                 
                 // Save the initialized config
                 sysInfoRepo.saveConfig(sInfo)
            }
            _systemInfo = sInfo
        }
        return _systemInfo!!
    }

    /**
     * 获取不可修改的设备基础信息
     */
    suspend fun getDeviceId(): String = ensureSystemInfo().deviceInfo!!.deviceId
    suspend fun getMacAddress(): String = ensureSystemInfo().deviceInfo!!.macAddress
    suspend fun getDeviceModel(): String = ensureSystemInfo().deviceInfo!!.model
    suspend fun getDeviceVersion(): String = ensureSystemInfo().deviceInfo!!.version
    suspend fun getDeviceName(): String = ensureSystemInfo().deviceInfo!!.name

    /**
     * 保存系统配置
     */
    suspend fun updateSystemInfo(info: SystemInfo) = mutex.withLock {
        sysInfoRepo.saveConfig(info)
        _systemInfo = info
    }

    /**
     * 加载系统配置
     */
    suspend fun getSystemInfo(): SystemInfo = mutex.withLock {
        ensureSystemInfo()
    }
}
