package com.airobotcomm.tablet.system

import android.content.Context
import android.provider.Settings
import com.airobotcomm.tablet.system.model.DeviceInfo
import com.airobotcomm.tablet.system.model.SystemConfig
import com.airobotcomm.tablet.system.repository.SysInfoRepo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * 配置业务逻辑类 - 处理基础设备信息展示与系统配置管理的业务规则
 * 遵循封闭原则，不对外直接暴露内部存储实现与模型细节
 */
@Singleton
class SystemManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sysInfoRepo: SysInfoRepo
) {
    private val mutex = Mutex()
    private var _deviceInfo: DeviceInfo? = null

    /**
     * 系统启动时调用的初始化方法，获取不可修改的设备基础信息
     */
    private fun ensureDeviceInfo(): DeviceInfo {
        if (_deviceInfo == null) {
            // 简单实现 MAC 获取逻辑，实际生产环境可能需要更复杂的兼容性处理
            // 这里暂用 androidId 的一部分或随机生成来模拟
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
            val mac = generateStableMac(androidId)

            _deviceInfo = DeviceInfo(
                name = "airobot-tablet",
                model = "airobot-tablet-V1",
                version = "1.0.0",
                deviceId = androidId,
                macAddress = mac,
                maxAirobot = 3
            )
        }
        return _deviceInfo!!
    }

    /**
     * 根据 androidId 生成稳定的 MAC 地址
     * 规则：
     * 1. 前缀从 ["fa:2e:39", "4c:da:59"] 中选择，通过 seed 的 hashCode 取模决定，确保重装后依然稳定
     * 2. 后 3 字节（6 位字符）使用 androidId 的末尾填充
     */
    private fun generateStableMac(seed: String): String {
        val prefixes = listOf("fa:2e:39", "4c:da:59")
        
        // 直接使用 hashCode 取模来选择前缀，避免使用 Random 类以确保绝对的确定性
        val index = abs(seed.hashCode()) % prefixes.size
        val prefix = prefixes[index]

        // 取 androidId 的最后 6 位作为 MAC 地址的后半部分
        // androidId 通常是 16 位十六进制字符串，如果不足 6 位则在前补 0
        val suffixSource = seed.takeLast(6).padStart(6, '0')
        val suffix = suffixSource.chunked(2).joinToString(":")

        return "$prefix:$suffix".lowercase()
    }

    /**
     * 获取不可修改的设备基础信息
     */
    fun getDeviceId(): String = ensureDeviceInfo().deviceId
    fun getMacAddress(): String = ensureDeviceInfo().macAddress
    fun getDeviceModel(): String = ensureDeviceInfo().model
    fun getDeviceVersion(): String = ensureDeviceInfo().version
    fun getDeviceName(): String = ensureDeviceInfo().name

    /**
     * 保存系统配置
     */
    suspend fun updateConfig(config: SystemConfig) = mutex.withLock {
        sysInfoRepo.saveConfig(config)
    }

    /**
     * 加载系统配置
     */
    suspend fun getConfig(): SystemConfig = mutex.withLock {
        sysInfoRepo.loadConfig()
    }
}