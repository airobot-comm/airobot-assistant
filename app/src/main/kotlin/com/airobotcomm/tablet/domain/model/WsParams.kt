package com.airobotcomm.tablet.domain.model

/**
 * WebSocket 连接参数类 - 由 OtaManager 提供给 NetworkService
 */
data class WsParams(
    val url: String,
    val token: String,
    val deviceId: String,
    val macAddress: String,
    val clientId: String,
    val clientName: String
)
