package com.airobot.core.system.model

import java.util.UUID

/**
 * 系统配置数据类 - 包含可修改的系统设置
 */
data class SystemInfo (
    // system device and auth info
    val serviceUrl: String = "https://api.tenclass.net/xiaozhi/ota/",
    val clientId: String = UUID.randomUUID().toString(),
    val deviceInfo: DeviceInfo = DeviceInfo.empty(),

    // airobot info，include agentinfo and role
    val aiAgent: AiAgent = AiAgent()
){
    // Convenience property for backward compatibility
    @Deprecated("Use deviceInfo.activation instead", ReplaceWith("deviceInfo.activation"))
    val activeInfo: ActiveInfo
        get() = deviceInfo.activation

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SystemInfo

        if (serviceUrl != other.serviceUrl) return false
        if (clientId != other.clientId) return false
        if (deviceInfo != other.deviceInfo) return false
        if (aiAgent != other.aiAgent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = serviceUrl.hashCode()
        result = 31 * result + clientId.hashCode()
        result = 31 * result + deviceInfo.hashCode()
        result = 31 * result + aiAgent.hashCode()
        return result
    }
}

