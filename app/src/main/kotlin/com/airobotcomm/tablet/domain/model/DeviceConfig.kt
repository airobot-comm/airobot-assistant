package com.airobotcomm.tablet.domain.model

import java.util.Random
import java.util.UUID

/**
 * 配置数据类
 */
data class DeviceConfig(
    val id: String,
    val name: String,
    val otaUrl: String,
    val websocketUrl: String = "",
    val macAddress: String,
    val clientId: String,
    val token: String,
    val activationCode: String = "",
    val mcpEnabled: Boolean = false
) {
    companion object {
        /**
         * 创建默认配置
         */
        fun createDefault(): DeviceConfig {
            return DeviceConfig(
                id = "default",
                name = "airobot-tablet",
                otaUrl = "",
                websocketUrl = "",
                clientId = generateAndroidId(),
                macAddress = generateMacAddress(),
                token = "test-token",
                activationCode = "",
                mcpEnabled = false
            )
        }

        /**
         * 使用Android_ID（SSAID），作为设备uuid
         */
        fun generateAndroidId(): String {
            //val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            return UUID.randomUUID().toString()
        }

        /**
         * 基于Android_ID（SSAID），及device.name生成MAC地址，确保mac独特且唯一
         */
        private fun generateMacAddress(): String {
            val random = Random()
            val mac = ByteArray(6)
            random.nextBytes(mac)
            return mac.joinToString(":") { "%02X".format(it) }
        }
    }
}