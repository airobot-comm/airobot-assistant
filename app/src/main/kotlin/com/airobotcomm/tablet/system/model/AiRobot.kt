package com.airobotcomm.tablet.system.model

import java.util.UUID

data class AiAgent(
    val agentUrl: String = "https://api.tenclass.net/xiaozhi/ota/",
    val agentId: String = UUID.randomUUID().toString(),
    val model: String = "gpt-3.5-turbo",
    val activationCode: String = "" // agent active code,such as xiaozhi
)

/**
 * agent服务的ota激活/动态认证获取新的通信凭证，目前支持ws
 * todo：后续要支持MQTT，支持V2的动态认证激活模式
 */
data class CommCredentials(
    val deviceId: String,
    val macAddress: String,
    val clientId: String,

    // websocket 连接参数
    val url: String,
    val token: String

    // mqtt 通信参数，待补充
)

/**
 * Ai机器人信息 - 智能体配置，角色名字，id，能力与模型等配置信息；
 * 可以配置不同智能体的多个airobot角色
 */
data class AiRobot(
    val roleName: String = "小美",                      // airobotActivate airobot's role-name：角色别名
    val roleId: String = UUID.randomUUID().toString(), // airobotActivate role-uuid
    val mcpEnabled: Boolean = false
){
   // AiRobot initialized with default values
}