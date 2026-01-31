package com.airobotcomm.tablet.system.model

import java.util.UUID

/**
 * 系统配置数据类 - 包含可修改的系统设置
 */
data class SystemInfo (
    val otaUrl: String = "https://api.tenclass.net/xiaozhi/ota/",
    val clientId: String = UUID.randomUUID().toString(),
    val deviceInfo: DeviceInfo,

    // activation and airobot info
    val activeInfo: ActiveInfo,
    val aiRobotNux: Byte = 3,     // airobot role number
    val aiRobotArray: Array<AiRobot?> = Array(size = aiRobotNux.toInt()) { null }
){
    // todo:完成构造方法赋值
}