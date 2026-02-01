package com.airobotcomm.tablet.system.model

import android.content.Context
import android.provider.Settings


/**
 * 设备基础数据类 - 包含系统启动时确定的不可修改的基础信息
 */
data class DeviceInfo(
    val name: String,
    val model: String,
    val version: String,
    val deviceId: String,
    val macAddress: String
){
    companion object {
        fun create(context: Context): DeviceInfo {
            var deviceId = generateDeviceId(context)
             return DeviceInfo(
                 name = "AiRobot-Assistant",
                 model = "AiRobot-Assistant-V1",
                 version = "1.0.0",
                 deviceId = deviceId,
                 macAddress = generateStableMac(deviceId)
             )
        }

        fun empty(): DeviceInfo {
            return DeviceInfo("", "", "", "", "")
        }

        /**
         * 根据规则生成clientId（先简单的直接用androidId）
         * todo：后续改为Android ID + 硬件指纹（主板 / CPU / 存储 ID）+ 应用签名 MD5
         */
        private fun generateDeviceId(context: Context): String {
            return Settings.Secure.getString(context.contentResolver,
                Settings.Secure.ANDROID_ID)
        }

        /**
         * 根据 androidId/deviceId 生成稳定的 MAC 地址
         * 规则：
         * 1. 前缀从 ["fa:2e:39", "4c:da:59"] 中选择，通过 seed 的 hashCode 取模决定，确保重装后依然稳定
         * 2. 后 3 字节（6 位字符）使用 androidId 的末尾填充
         */
        private fun generateStableMac(seed: String): String {
            val prefixes = listOf("fa:2e:39", "4c:da:59")

            // 直接使用 hashCode 取模来选择前缀，避免使用 Random 类以确保绝对的确定性
            val index = kotlin.math.abs(seed.hashCode()) % prefixes.size
            val prefix = prefixes[index]

            // 取 androidId 的最后 6 位作为 MAC 地址的后半部分
            // androidId 通常是 16 位十六进制字符串，如果不足 6 位则在前补 0
            val suffixSource = seed.takeLast(6).padStart(6, '0')
            val suffix = suffixSource.chunked(2).joinToString(":")

            return "$prefix:$suffix".lowercase()
        }
    }
}
