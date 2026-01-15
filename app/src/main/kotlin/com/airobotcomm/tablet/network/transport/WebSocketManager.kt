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
    data class BinaryMessage(val data: ByteArray) : WebSocketEvent()
    data class Error(val error: String) : WebSocketEvent()
}

/**
 * WebSocket管理器
 */
class WebSocketManager(private val context: Context) {

    companion object {
        private const val TAG = "WebSocketManager"
        private const val BASE_RECONNECT_DELAY = 2000L 
        private const val MAX_RECONNECT_DELAY = 60000L // 最大重连延迟60秒
        private const val CONNECT_TIMEOUT = 15L 
        private const val WRITE_TIMEOUT = 15L 
    }

    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var shouldReconnect = true
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 连接参数，用于自动重连
    private var lastUrl: String? = null
    private var lastDeviceId: String? = null
    private var lastToken: String? = null
    
    // 重连管理
    private var currentRetryAttempt = 0

    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 1)
    val events: SharedFlow<WebSocketEvent> = _events

    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS) // Step 1: 启用 OkHttp 内置 Ping/Pong
        .build()

    /**
     * 连接WebSocket
     */
    fun connect(url: String, deviceId: String, token: String) {
        Log.d(TAG, "正在连接WebSocket: $url")
        
        // 在发起新连接前，取消任何旧的或正在尝试的连接，防止多连接和资源泄漏
        webSocket?.cancel()
        webSocket = null
        isConnected = false

        // 如果url或设备ID发生了变化，视为一次新的连接，重置计数器
        if (lastUrl != url || lastDeviceId != deviceId) {
            currentRetryAttempt = 0
        }

        // 保存连接参数用于自动重连
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

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket连接成功")
                isConnected = true
                // 移除重置 currentRetryAttempt 的逻辑。连接成功并不意味着握手成功，
                // 只有当连接完全稳定后才能重置，但最安全的方法是让外部逻辑在用户
                // 主动连接时重置，或让重连逻辑自己递增。
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
                retryConnection()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket已关闭: $code - $reason")
                retryConnection()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket连接失败", t)
                scope.launch {
                    _events.emit(WebSocketEvent.Error("连接失败"))
                }
                retryConnection()
            }
        })
    }

    /**
     * 发送原始字符串消息
     */
    fun sendTextMessage(message: String) {
        if (isConnected && webSocket != null) {
            try {
                val success = webSocket!!.send(message)
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
        if (isConnected && webSocket != null) {
            try {
                val success = webSocket!!.send(ByteString.of(*data))
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
     * ws重连机制
     */
    private fun retryConnection() {
        if (!shouldReconnect || lastUrl == null || lastDeviceId == null || lastToken == null) {
            return
        }

        Log.d(TAG, "连接丢失/关闭，准备自动重连...")
        isConnected = false

        scope.launch {
            // 在计算延迟前递增计数器，确保每次计算都使用下一个重试编号。
            // 之前的代码是在 delay 之后递增，可能导致并发问题或计数不准确。
            currentRetryAttempt++

            // 计算指数退避延迟
            val delayMs = MAX_RECONNECT_DELAY.coerceAtMost(
                BASE_RECONNECT_DELAY *
                        (2.0.pow(currentRetryAttempt.toDouble()).toLong())
            )

            Log.d(TAG, "将在 ${delayMs}ms 后进行第 ${currentRetryAttempt + 1} 次重连尝试")
            delay(delayMs)

            connect(lastUrl!!, lastDeviceId!!, lastToken!!)
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        shouldReconnect = false
        webSocket?.close(1000, "正常关闭")
        webSocket = null
        isConnected = false
        currentRetryAttempt = 0
        // 清理连接参数
        lastUrl = null
        lastDeviceId = null
        lastToken = null
    }

    /**
     * 检查连接状态
     */
    fun isConnected(): Boolean = isConnected

    /**
     * 重新启用自动重连
     */
    fun enableReconnect() {
        shouldReconnect = true
    }

    /**
     * 禁用自动重连
     */
    fun disableReconnect() {
        shouldReconnect = false
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
}