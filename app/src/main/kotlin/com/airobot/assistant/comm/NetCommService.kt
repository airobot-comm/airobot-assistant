package com.airobot.assistant.comm

import kotlinx.coroutines.flow.StateFlow

/**
 * AiRobot 网络通信协议事件
 */
sealed class NetCommEvent {
    // 通信状态
    object Connected : NetCommEvent()
    object Disconnected : NetCommEvent()
    data class Error(val message: String) : NetCommEvent()

    // 业务事件: 对话协议
    data class ActivationRequired(val code: String) : NetCommEvent()
    data class STT(val text: String) : NetCommEvent()
    data class TtsStart(val sessionId: String) : NetCommEvent()
    data class TtsStop(val sessionId: String) : NetCommEvent()
    data class TtsSentence(val text: String) : NetCommEvent()
    data class LLM(val emotion: String?, val text: String?) : NetCommEvent()
    data class IoT(val command: String) : NetCommEvent()
    object DialogueEnd : NetCommEvent()

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
    
    // 协议层透明转发
    fun startListening(mode: String = "auto")
    fun stopListening()
    fun sendAudio(data: ByteArray)
    fun sendText(text: String)
    fun abort(reason: String = "user_interrupt")
}

