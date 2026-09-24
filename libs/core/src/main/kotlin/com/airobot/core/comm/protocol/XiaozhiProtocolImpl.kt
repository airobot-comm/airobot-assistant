package com.airobot.core.comm.protocol

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton
import com.airobot.core.comm.NetCommEvent

/**
 * Xiaozhi agent communication protocol implementation.
 * Handles handshake (Hello), packet encapsulation, and session management.
 */
@Singleton
class XiaozhiProtocolImpl @Inject constructor() : CommProtocol {
    companion object {
        private const val TAG = "XiaozhiProtocol"
        private const val HELLO_TIMEOUT = 15000L
    }

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _events = MutableSharedFlow<NetCommEvent>(replay = 0)
    override val events: SharedFlow<NetCommEvent> = _events.asSharedFlow()

    private var sessionId: String? = null
    private var isHandshakeComplete = false
    private var helloTimeoutJob: Job? = null

    // Callback set by NetCommService to send raw text
    private var onSendRawText: ((String) -> Unit)? = null

    override fun setRawSender(sender: (String) -> Unit) {
        onSendRawText = sender
    }

    override fun open(url: String, deviceId: String, token: String) {
        reset()
        sendHello()
        startHelloTimeout()
    }

    override fun close() {
        reset()
    }

    /**
     * Reset protocol states
     */
    private fun reset() {
        sessionId = null
        isHandshakeComplete = false
        helloTimeoutJob?.cancel()
    }

    /**
     * Handle raw text messages received from WebSocket
     */
    override fun handleRawText(text: String) {
        try {
            val json = gson.fromJson(text, JsonObject::class.java)
            val type = json.get("type")?.asString

            if (type == "hello") {
                handleHelloResponse(json)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle protocol text: $text", e)
        }
    }

    private fun handleHelloResponse(json: JsonObject) {
        val transport = json.get("transport")?.asString
        if (transport == "websocket") {
            sessionId = json.get("session_id")?.asString
            isHandshakeComplete = true
            helloTimeoutJob?.cancel()
            Log.d(TAG, "Xiaozhi protocol handshake successful, sessionId: $sessionId")
            scope.launch {
                _events.emit(NetCommEvent.Connected)
            }
        } else {
            Log.e(TAG, "Handshake failed: transport mismatch")
            scope.launch {
                _events.emit(NetCommEvent.Error("Protocol Handshake Failed: transport mismatch"))
            }
        }
    }

    private fun startHelloTimeout() {
        helloTimeoutJob = scope.launch {
            delay(HELLO_TIMEOUT)
            if (!isHandshakeComplete) {
                Log.e(TAG, "Xiaozhi handshake protocol timeout")
                _events.emit(NetCommEvent.Error("Protocol Handshake Timeout"))
            }
        }
    }

    private fun sendHello() {
        val hello = JsonObject().apply {
            addProperty("type", "hello")
            addProperty("version", 1)
            addProperty("transport", "websocket")
            add("audio_params", JsonObject().apply {
                addProperty("format", "opus")
                addProperty("sample_rate", 16000)
                addProperty("channels", 1)
                addProperty("frame_duration", 60)
            })
            // Enable MCP protocol support
            add("features", JsonObject().apply {
                addProperty("mcp", true)
            })
        }
        val helloJson = gson.toJson(hello)
        Log.d(TAG, "Sending hello packet: $helloJson")
        onSendRawText?.invoke(helloJson)
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
        // Audio frames are sent directly via transport, protocol class acts as placeholder
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
