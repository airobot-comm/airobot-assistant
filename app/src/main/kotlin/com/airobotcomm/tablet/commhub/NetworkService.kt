package com.airobotcomm.tablet.commhub

import com.airobotcomm.tablet.commhub.protocol.AiRobotEvent
import kotlinx.coroutines.flow.StateFlow

/**
 * 网络连接状态
 */
enum class NetworkState {
    IDLE,
    INITIALIZING, // OTA/报备中
    CONNECTING,   // WS连接中
    CONNECTED,    // 已连接并握手完成
    ERROR,
    RECONNECTING
}

/**
 * 统一网络服务接口
 */
interface NetworkService {
    val state: StateFlow<NetworkState>
    val events: kotlinx.coroutines.flow.SharedFlow<AiRobotEvent>
    val isConnected: Boolean

    /**
     * 初始化并连接网络（自动执行OTA并建立WebSocket）
     */
    fun connect()
    
    fun disconnect()
    
    /**
     * 用户确认激活后继续连接
     */
    fun onActivationConfirmed()

    // 协议层透明转发
    fun startListening(mode: String = "auto")
    fun stopListening()
    fun sendAudio(data: ByteArray)
    fun sendText(text: String)
    fun abort(reason: String = "user_interrupt")
}
