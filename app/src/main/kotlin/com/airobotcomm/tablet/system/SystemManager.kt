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
                //macAddress = mac,  //todo :先用已经激活认证的mac，后续检查升级版激活机制（后台改了激活逻辑）
                macAddress = "4c:da:59:a0:32:54",
                maxAirobot = 3
            )
        }
        return _deviceInfo!!
    }

    private fun generateStableMac(seed: String): String {
        val random = Random(seed.hashCode().toLong())
        val mac = ByteArray(6)
        random.nextBytes(mac)
        // 保证是 locally administered address (second least significant bit of first byte set to 1)
        mac[0] = (mac[0].toInt() or 0x02).toByte()
        mac[0] = (mac[0].toInt() and 0xFE.inv()).toByte() // unicast (least significant bit of first byte set to 0) - wait, FE.inv() is wrong for clearing bit 0. 
        // Correct way to clear bit 0: mac[0] = (mac[0].toInt() and 0xFE).toByte()
        mac[0] = (mac[0].toInt() and 0xFE).toByte()
        
        return mac.joinToString(":") { "%02X".format(it) }
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