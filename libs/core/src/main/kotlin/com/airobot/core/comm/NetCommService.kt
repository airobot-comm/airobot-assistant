package com.airobot.core.comm

import kotlinx.coroutines.flow.StateFlow

/**
 * AiRobot 网络通信协议事件
 */
sealed class NetCommEvent {
    // 通信状态
    object Connected : NetCommEvent()
    object Disconnected : NetCommEvent()
    data class Error(val message: String) : NetCommEvent()

    // 业务事件: 原始数据与对话控制
    data class ActivationRequired(val code: String) : NetCommEvent()
    data class TextMessage(val json: String) : NetCommEvent()

    // 原始数据
    data class AudioFrame(val data: ByteArray) : NetCommEvent()
}

/**
 * 网络连接状态
 */
enum class NetworkState {
    IDLE,
    CONNECTING,   // WS连接中
    CONNECTED,    // 已连接并握手完成
    ERROR,
    RECONNECTING
}

/**
 * 统一网络通信服务接口
 */
interface NetCommService {
    val state: StateFlow<NetworkState>
    val events: kotlinx.coroutines.flow.SharedFlow<NetCommEvent>
    val isConnected: Boolean

    /**
     * 连接到 WebSocket 服务
     */
    fun connect()
    
    fun disconnect()
    
    // Protocol-layer command forwarding
    fun startListening(mode: String = "auto")
    fun stopListening()
    fun sendAudio(data: ByteArray)
    fun sendText(text: String)
    fun abort(reason: String = "user_interrupt")

    /**
     * Send raw text directly to WebSocket, bypassing protocol-layer wrapping.
     * Used for MCP JSON-RPC responses that must not be re-framed.
     */
    fun sendRawText(text: String)
}


