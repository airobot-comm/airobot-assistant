package com.airobotcomm.tablet.system.model

/**
 * 设备基础数据类 - 包含系统启动时确定的不可修改的基础信息
 */
data class DeviceInfo(
    val name: String = "AiRobot-Assistant",
    val model: String = "AiRobot-Assistant-V1",
    val version: String = "1.0.0",
    val deviceId: String,
    val macAddress: String,
){
    // todo：完善构造函数，实现自我赋值：mac，name,model,version,deviceId,Mac扽等


    // todo: mac的生成，改为内部fun，对外不再暴漏
}
