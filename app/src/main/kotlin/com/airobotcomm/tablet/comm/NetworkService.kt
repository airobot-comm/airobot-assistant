package com.airobotcomm.tablet.comm

import com.airobotcomm.tablet.comm.protocol.AiRobotEvent
import kotlinx.coroutines.flow.StateFlow

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
 * 统一网络服务接口
 */
interface NetworkService {
    val state: StateFlow<NetworkState>
    val events: kotlinx.coroutines.flow.SharedFlow<AiRobotEvent>
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
