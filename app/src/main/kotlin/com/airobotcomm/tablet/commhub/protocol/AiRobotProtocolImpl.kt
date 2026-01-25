package com.airobotcomm.tablet.commhub.protocol

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AiRobot 通信协议的具体实现
 * 负责手握手 (Hello)、业务消息封装和会话管理
 */
@Singleton
class AiRobotProtocolImpl @Inject constructor() : AiRobotProtocol {
    companion object {
        private const val TAG = "AiRobotProtocol"
        private const val HELLO_TIMEOUT = 15000L
    }

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _events = MutableSharedFlow<AiRobotEvent>(replay = 0)
    override val events: SharedFlow<AiRobotEvent> = _events.asSharedFlow()

    private var sessionId: String? = null
    private var isHandshakeComplete = false
    private var helloTimeoutJob: Job? = null

    // 回调，由 NetworkService 设置，用于发送原始文本
    private var onSendRawText: ((String) -> Unit)? = null

    override fun setRawSender(sender: (String) -> Unit) {
        onSendRawText = sender
    }

    override fun open(url: String, deviceId: String, token: String) {
        // 重置并开始握手流程
        reset()
        sendHello()
        startHelloTimeout()
    }

    override fun close() {
        reset()
    }

    /**
     * 重置协议状态
     */
    private fun reset() {
        sessionId = null
        isHandshakeComplete = false
        helloTimeoutJob?.cancel()
    }

    /**
     * 处理收到的原始文本消息
     */
    override fun handleRawText(text: String) {
        try {
            val json = gson.fromJson(text, JsonObject::class.java)
            val type = json.get("type")?.asString

            if (type == "hello") {
                handleHelloResponse(json)
            }
        } catch (e: Exception) {
            Log.e(TAG, "协议处理消息失败: $text", e)
        }
    }

    private fun handleHelloResponse(json: JsonObject) {
        val transport = json.get("transport")?.asString
        if (transport == "websocket") {
            sessionId = json.get("session_id")?.asString
            isHandshakeComplete = true
            helloTimeoutJob?.cancel()
            Log.d(TAG, "AiRobot 协议握手成功, sessionId: $sessionId")
            scope.launch {
                _events.emit(AiRobotEvent.Connected)
            }
        } else {
            Log.e(TAG, "握手失败: transport 不匹配")
            scope.launch {
                _events.emit(AiRobotEvent.Error("Protocol Handshake Failed: transport mismatch"))
            }
        }
    }

    private fun startHelloTimeout() {
        helloTimeoutJob = scope.launch {
            delay(HELLO_TIMEOUT)
            if (!isHandshakeComplete) {
                Log.e(TAG, "AiRobot 握手协议超时")
                _events.emit(AiRobotEvent.Error("Protocol Handshake Timeout"))
            }
        }
    }

    private fun sendHello() {
        val hello = JsonObject().apply {
            addProperty("type", "hello")
            addProperty("version", 1)
            addProperty("connect", "websocket")
            add("audio_params", JsonObject().apply {
                addProperty("format", "opus")
                addProperty("sample_rate", 16000)
                addProperty("channels", 1)
                addProperty("frame_duration", 60)
            })
        }
        onSendRawText?.invoke(gson.toJson(hello))
    }

    override fun startListening(mode: String) {
        val message = JsonObject().apply {
            sessionId?.let { addProperty("session_id", it) }
            addProperty("type", "listen")
            addProperty("state", "start")
            addProperty("mode", mode)
        }
        onSendRawText?.invoke(gson.toJson(message))
    }

    override fun stopListening() {
        val message = JsonObject().apply {
            sessionId?.let { addProperty("session_id", it) }
            addProperty("type", "listen")
            addProperty("state", "stop")
        }
        onSendRawText?.invoke(gson.toJson(message))
    }

    override fun sendAudio(data: ByteArray) {
        // 音频数据通常直接由 connect 层发送，协议层仅作为逻辑占位
    }

    override fun sendText(text: String) {
        val message = JsonObject().apply {
            sessionId?.let { addProperty("session_id", it) }
            addProperty("type", "listen")
            addProperty("state", "detect")
            addProperty("text", text)
            addProperty("source", "text")
        }
        onSendRawText?.invoke(gson.toJson(message))
    }

    override fun abort(reason: String) {
        val message = JsonObject().apply {
            sessionId?.let { addProperty("session_id", it) }
            addProperty("type", "abort")
            addProperty("reason", reason)
        }
        onSendRawText?.invoke(gson.toJson(message))
    }
}