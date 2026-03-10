package com.airobot.assistant.comm.protocol

import com.airobot.assistant.comm.NetCommEvent
import kotlinx.coroutines.flow.SharedFlow

/**
 * AiRobot 通信协议接口
 */
interface CommProtocol {
    val events: SharedFlow<NetCommEvent>
    
    // 管线连接：由底层传输层调用或设置
    fun setRawSender(sender: (String) -> Unit)
    fun handleRawText(text: String)

    fun open(url: String, deviceId: String, token: String)
    fun close()
    
    fun startListening(mode: String = "auto")
    fun stopListening()
    fun sendAudio(data: ByteArray)
    fun sendText(text: String)
    fun abort(reason: String = "user_interrupt")
}

