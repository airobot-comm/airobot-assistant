package com.airobotcomm.tablet.system.model

/**
 * 设备基础数据类 - 包含系统启动时确定的不可修改的基础信息
 */
data class DeviceInfo(
    val name: String,
    val model: String,
    val version: String,
    val deviceId: String,
    val macAddress: String,
    val maxAirobot: Byte = 3 // 最大airobot角色数量
)
