package com.airobotcomm.tablet.system.model

import java.util.UUID
import javax.crypto.SecretKey

/**
 * agent服务的ota认证/激活而动态获取新的通信凭证
 * 目前支持ws，后续要支持mqtt，支持agent V2动态认证
 */
data class CommCredentials(
    // websocket 连接参数
    val url: String,
    val token: String,

    // mqtt 通信参数（todo：后续扩展支持，支持V2的动态认证激活模式）
    val clientId: String,
    val topic: String,
    val qos: Int
)

data class AiAgent(
    val agentUrl: String = "https://api.tenclass.net/xiaozhi/ota/",
    val agentId: String = UUID.randomUUID().toString(),
    val model: String = "qwen-3.5-test",

    // agent active code and comm credentials(such as xiaozhi)
    val activationCode: String = "",
    val commCredentials: CommCredentials? = null,

    // mcp support
    val mcpEnabled: Boolean = false,
)

/**
 * Ai机器人信息 - 智能体配置，角色名字，id，agent配置信息；
 * 可以配置不同智能体的多个airobot角色
 */
data class AiRobot(
    // airobot role
    val roleName: String = "小美",
    val roleId: String = UUID.randomUUID().toString(), // airobotActivate role-uuid

    // agent of airobot
    val aiAgent: AiAgent = AiAgent()
){
   // AiRobot initialized with default values
}