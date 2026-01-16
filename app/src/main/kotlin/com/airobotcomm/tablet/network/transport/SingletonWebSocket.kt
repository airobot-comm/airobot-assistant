package com.airobotcomm.tablet.network.transport

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit
import kotlin.math.pow

/**
 * WebSocket事件类型
 */
sealed class WebSocketEvent {
    object Connected : WebSocketEvent()
    object Disconnected : WebSocketEvent()
    data class TextMessage(val message: String) : WebSocketEvent()
    data class BinaryMessage(val data: ByteArray) : WebSocketEvent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as BinaryMessage

            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }
    }

    data class Error(val error: String) : WebSocketEvent()
}

/**
 * 非静态的伪单例模式WebSocket管理器
 */
class SingletonWebSocket(context: Context) {
    companion object {
        private const val TAG = "SingletonWebSocket"
        private const val NORMAL_CLOSE_CODE = 1000
        private const val BASE_RECONNECT_DELAY = 2000L
        private const val MAX_RECONNECT_DELAY = 60000L // 最大重连延迟60秒
        private const val CONNECT_TIMEOUT = 15L 
        private const val WRITE_TIMEOUT = 15L 
    }

    enum class SocketState {
        IDLE,        // 无连接
        CONNECTING,  // 连接中
        CONNECTED,   // 已连接
        RECONNECTING // 重连等待中
    }

    // 一个非静态伪单例ws，设置连接状态确保同时只有一个连接，并启用Ping/Pong保持长连接
    private var webSocketSingleton: WebSocket? = null
    private var currentState = SocketState.IDLE
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 1)
    val events: SharedFlow<WebSocketEvent> = _events
    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    // 采用自动的重连机制，内建重连唯一性机制，确保单一连接稳定性
    private var reconnectModel = true
    private var reconnectJob: Job? = null
    private var currentRetryAttempt = 0
    private var lastUrl: String? = null
    private var lastDeviceId: String? = null
    private var lastToken: String? = null

    /**
     * 连接WebSocket
     */
    fun connect(url: String, deviceId: String, token: String) {
        // 如果正在连接或连接正常工作，且是同一个连接，则不需要发起连接，直接返回
        if (webSocketSingleton != null
            && currentState == SocketState.CONNECTED
            && currentState == SocketState.CONNECTING
            && webSocketSingleton!!.request().url.toString() == url){
            return
        }

        // 在发起新连接前，取消任何旧的或正在尝试的重连接，确保连接唯一性
        webSocketSingleton?.cancel()
        reconnectJob?.cancel()
        currentState = SocketState.CONNECTING
        Log.d(TAG, "正在连接WebSocket: $url")

        // 保存新的连接参数，用于自动重连
        lastUrl = url
        lastDeviceId = deviceId
        lastToken = token

        val request = Request.Builder()
            .url(url)
            .addHeader("Device-Id", deviceId)
            .addHeader("Client-Id", deviceId)
            .addHeader("Protocol-Version", "1")
            .addHeader("Authorization", "Bearer $token")
            .build()
        webSocketSingleton = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // 连接成功，设置状态，重置重连计数器，取消可能的重连任务
                currentState = SocketState.CONNECTED
                currentRetryAttempt = 0
                reconnectJob?.cancel()

                // 发送连接成功事件
                Log.d(TAG, "WebSocket连接成功")
                scope.launch {
                    _events.emit(WebSocketEvent.Connected)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "收到文本消息: $text")
                scope.launch {
                    _events.emit(WebSocketEvent.TextMessage(text))
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "收到二进制消息，长度: ${bytes.size}")
                scope.launch {
                    _events.emit(WebSocketEvent.BinaryMessage(bytes.toByteArray()))
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket正在关闭: $code - $reason")

                // 收到服务器关闭，立即关闭而不是等待超时（不要使用close协商导致onClosed重复响应）
                webSocket.cancel()
                reconnectWithBackoff() // 作单例长连接模式，立即触发“指数退避重连”逻辑
                scope.launch {
                    _events.emit(WebSocketEvent.Disconnected)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket已关闭: $code - $reason")

                // 如果正常关闭连接，不用强制重连，等待上层重连
                if (NORMAL_CLOSE_CODE == code) {
                    currentState = SocketState.IDLE
                    scope.cancel()
                }

                // 作单例模式长连接，立即触发“指数退避重连”逻辑
                reconnectWithBackoff()
                scope.launch {
                    _events.emit(WebSocketEvent.Disconnected)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket连接失败：抛出异常", t)
                scope.launch {
                    _events.emit(WebSocketEvent.Error("连接失败"))
                }

                // 连接失败情况下，需要立即避退重连
                reconnectWithBackoff()
            }
        })
    }

    /**
     * 指数避退重连
     */
    private fun reconnectWithBackoff() {
        // 如果已有重连任务在跑，就不要再启动新的了，确保不能并发
        if (!reconnectModel || lastUrl == null
            || lastDeviceId == null || lastToken == null
            || reconnectJob?.isActive == true) {
            return
        }
        Log.d(TAG, "连接失败/丢失，准备自动避退重连...")

        currentState = SocketState.RECONNECTING
        reconnectJob = scope.launch {
            // 计算指数退避延迟
            val delayMs = MAX_RECONNECT_DELAY.coerceAtMost(
                BASE_RECONNECT_DELAY *
                        (2.0.pow(currentRetryAttempt.toDouble()).toLong())
            )
            currentRetryAttempt++ //在计算延时后再递增
            Log.d(TAG, "将在 ${delayMs}ms 后进行第 ${currentRetryAttempt} 次重连尝试")
            delay(delayMs)
            connect(lastUrl!!, lastDeviceId!!, lastToken!!)
        }
    }

    /**
     * 发送原始字符串消息
     */
    fun sendTextMessage(message: String) {
        if (isConnected() && webSocketSingleton != null) {
            try {
                val success = webSocketSingleton!!.send(message)
                if (success) {
                    Log.d(TAG, "发送文本消息: $message")
                } else {
                    Log.w(TAG, "发送文本消息失败，WebSocket可能已关闭")
                }
            } catch (e: Exception) {
                Log.e(TAG, "发送文本消息异常", e)
            }
        } else {
            Log.w(TAG, "WebSocket未连接，无法发送消息")
        }
    }

    /**
     * 发送二进制消息
     */
    fun sendBinaryMessage(data: ByteArray) {
        if (isConnected() && webSocketSingleton != null) {
            try {
                val success = webSocketSingleton!!.send(ByteString.of(*data))
                if (success) {
                    Log.d(TAG, "发送二进制消息，长度: ${data.size}")
                } else {
                    Log.w(TAG, "发送二进制消息失败，WebSocket可能已关闭")
                }
            } catch (e: Exception) {
                Log.e(TAG, "发送二进制消息异常", e)
            }
        } else {
            Log.w(TAG, "WebSocket未就绪，无法发送二进制消息")
        }
    }

    /**
     * 检查连接状态
     */
    fun isConnected(): Boolean {
        return currentState == SocketState.CONNECTED
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        Log.d(TAG, "websocket断开清除")

        //清理连接资源
        webSocketSingleton?.cancel()
        reconnectJob?.cancel()
        scope.cancel()

        // 重置连接参数
        lastUrl = null
        lastDeviceId = null
        lastToken = null
        webSocketSingleton = null
        currentState = SocketState.IDLE
        currentRetryAttempt = 0
    }
}