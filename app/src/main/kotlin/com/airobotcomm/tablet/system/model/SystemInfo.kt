package com.airobotcomm.tablet.system.model

import java.util.UUID

/**
 * 系统配置数据类 - 包含可修改的系统设置
 */
data class SystemInfo (
    // system device and auth info
    val serviceUrl: String = "https://api.tenclass.net/xiaozhi/ota/",
    val clientId: String = UUID.randomUUID().toString(),
    val deviceInfo: DeviceInfo = DeviceInfo.empty(),
    val activeInfo: ActiveInfo = ActiveInfo(productKey = "", secretKey = "", time = ""),

    // airobot info，include agentinfo and role
    val aiAgent: AiAgent = AiAgent(),
    val aiRobotNux: Byte = 3,     // airobot role number
    val aiRobotArray: Array<AiRobot?> = Array(size = aiRobotNux.toInt()) { null }
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SystemInfo

        if (serviceUrl != other.serviceUrl) return false
        if (clientId != other.clientId) return false
        if (deviceInfo != other.deviceInfo) return false
        if (activeInfo != other.activeInfo) return false
        if (aiRobotNux != other.aiRobotNux) return false
        if (!aiRobotArray.contentEquals(other.aiRobotArray)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = serviceUrl.hashCode()
        result = 31 * result + clientId.hashCode()
        result = 31 * result + deviceInfo.hashCode()
        result = 31 * result + activeInfo.hashCode()
        result = 31 * result + aiRobotNux
        result = 31 * result + aiRobotArray.contentHashCode()
        return result
    }
}