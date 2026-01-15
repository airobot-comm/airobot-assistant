package com.airobotcomm.tablet.network.protocol

import kotlinx.coroutines.flow.SharedFlow

/**
 * AiRobot 通信协议事件
 */
sealed class AiRobotEvent {
    object Connected : AiRobotEvent()
    object Disconnected : AiRobotEvent()
    data class Error(val message: String) : AiRobotEvent()
    
    // 业务事件
    data class ActivationRequired(val code: String) : AiRobotEvent()
    data class STT(val text: String) : AiRobotEvent()
    data class TtsStart(val sessionId: String) : AiRobotEvent()
    data class TtsStop(val sessionId: String) : AiRobotEvent()
    data class TtsSentence(val text: String) : AiRobotEvent()
    data class LLM(val emotion: String?, val text: String?) : AiRobotEvent()
    data class IoT(val command: String) : AiRobotEvent()
    object DialogueEnd : AiRobotEvent()
    
    // 原始数据
    data class AudioFrame(val data: ByteArray) : AiRobotEvent()
}

/**
 * AiRobot 通信协议接口
 */
interface AiRobotProtocol {
    val events: SharedFlow<AiRobotEvent>
    
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
