package com.airobot.core.system.model

import java.util.UUID

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
    val agentVendor: String = "xiaozhi-ai", // todo:agent vendor will support coze and joy-agent
    val agentUrl: String = "https://api.tenclass.net/xiaozhi/ota/",
    val agentId: String = UUID.randomUUID().toString(),

    // agent active code and comm credentials(such as xiaozhi)
    val activationCode: String = "",
    val commCredentials: CommCredentials? = null,

    // mcp support
    val mcpEnabled: Boolean = false,
)


