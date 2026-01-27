package com.airobotcomm.tablet.domain.model

import java.util.Random
import java.util.UUID

/**
 * 设备基础数据类
 */
data class DeviceInfo(
    val deviceModel: String,
    val name: String,
    val otaUrl: String,
    val websocketUrl: String = "",
    val deviceId: String,
    val clientId: String,
    val token: String,
    val activationCode: String = "",
    val mcpEnabled: Boolean = false
) {
    companion object {
        /**
         * 创建默认配置
         */
        fun createDefault(): DeviceInfo {
            return DeviceInfo(
                deviceModel = "airobot-tablet-V1",
                name = "airobot-tablet",
                otaUrl = "",
                websocketUrl = "",
                deviceId = generateDeviceId(),
                clientId = generateClientId(),
                token = "test-token",
                activationCode = "",
                mcpEnabled = false
            )
        }

        /**
         * 目前的设备id本质是一个mac地址；
         * 基于Android_ID（SSAID），及device.name生成MAC地址，确保mac独特且唯一
         */
        private fun generateDeviceId(): String {
            // todo 修改代码实现基于Android_ID（SSAID）的device-mac
            val random = Random()
            val mac = ByteArray(6)
            random.nextBytes(mac)
            return mac.joinToString(":") { "%02X".format(it) }
        }

        /**
         * 目前的client-id是一个UUID；
         * 使用deviceId作为client-uuid参数，计算生成client uuid
         */
        fun generateClientId(): String {
            // todo 修改代码实现基于deviceId的client-uuid
            //val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            return UUID.randomUUID().toString()
        }
    }
}