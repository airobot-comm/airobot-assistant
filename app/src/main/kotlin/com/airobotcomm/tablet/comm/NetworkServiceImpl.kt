package com.airobotcomm.tablet.comm

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.airobotcomm.tablet.system.OtaManager
import com.airobotcomm.tablet.comm.protocol.AiRobotEvent
import com.airobotcomm.tablet.comm.protocol.AiRobotProtocol
import com.airobotcomm.tablet.comm.protocol.ProtocolAdapter
import com.airobotcomm.tablet.comm.transport.ConnectivityMonitor
import com.airobotcomm.tablet.comm.transport.WebSocketEvent
import com.airobotcomm.tablet.comm.transport.SingletonWebSocket

@Singleton
class NetworkServiceImpl @Inject constructor(
    private val singletonWebSocket: SingletonWebSocket,
    private val otaManager: OtaManager,
    private val protocolAdapter: ProtocolAdapter,
    private val connectivityMonitor: ConnectivityMonitor,
    private val protocol: AiRobotProtocol
) : NetworkService {

    companion object {
        private const val TAG = "NetworkService"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _state = MutableStateFlow(NetworkState.IDLE)
    override val state: StateFlow<NetworkState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AiRobotEvent>(replay = 0)
    override val events: SharedFlow<AiRobotEvent> = _events.asSharedFlow()

    override val isConnected: Boolean
        get() = singletonWebSocket.isConnected()

    init {
        // 配置协议层的发送回调
        protocol.setRawSender { text ->
            singletonWebSocket.sendTextMessage(text)
        }

        // 桥接传输层事件
        scope.launch {
            singletonWebSocket.events.collect { wsEvent ->
                when (wsEvent) {
                    is WebSocketEvent.Connected -> {
                        _state.value = NetworkState.CONNECTING // 传输层 OK，进入协议握手
                        val params = otaManager.getWsConnectionParams()
                        runBlocking { // waring：使用runBlocking来处理协议握手，确保握手后发送数据
                            protocol.open("", params.deviceId, params.token)
                        }
                    }
                    is WebSocketEvent.Reconnecting -> {
                        _state.value = NetworkState.RECONNECTING
                        protocol.close()
                        _events.emit(AiRobotEvent.Disconnected)
                    }
                    is WebSocketEvent.TextMessage -> {
                        // 1. 交给协议层处理握手等控制逻辑
                        protocol.handleRawText(wsEvent.message)
                        // 2. 交给适配器解析业务数据
                        protocolAdapter.parseTextMessage(wsEvent.message)?.let { businessEvent ->
                            _events.emit(businessEvent)
                        }
                    }
                    is WebSocketEvent.BinaryMessage -> {
                        _events.emit(AiRobotEvent.AudioFrame(wsEvent.data))
                    }
                }
            }
        }

        // 桥接协议层高层事件
        scope.launch {
            protocol.events.collect { protocolEvent ->
                if (protocolEvent is AiRobotEvent.Connected) {
                    _state.value = NetworkState.CONNECTED
                }
                _events.emit(protocolEvent)
            }
        }

        // 监听系统网络变化并尝试自动恢复
        scope.launch {
            connectivityMonitor.isNetworkAvailable.collect { isAvailable ->
                if (isAvailable && !isConnected && _state.value != NetworkState.IDLE) {
                    Log.d(TAG, "检测到网络恢复，触发自动连接恢复")
                    connect()
                }
            }
        }
    }

    override fun connect() {
        scope.launch {
            val params = otaManager.getWsConnectionParams()
            if (params.url.isBlank()) {
                _state.value = NetworkState.ERROR
                _events.tryEmit(AiRobotEvent.Error("WebSocket URL is empty. Please check OTA/Activation."))
                return@launch
            }

            _state.value = NetworkState.CONNECTING
            try {
                connectInternal(params.url, params.deviceId, params.clientId,params.token)
            } catch (e: Exception) {
                _state.value = NetworkState.ERROR
                _events.tryEmit(AiRobotEvent.Error(e.message ?: "Connection failed"))
            }
        }
    }

    override fun disconnect() {
        singletonWebSocket.close()
        _state.value = NetworkState.IDLE
    }


    override fun sendAudio(data: ByteArray) {
        Log.d(TAG, "发送二进制消息，长度: ${data.size}")
        // todo 发送语音数据需要优化，通过VAD检测减少发送数据，并做cache排队防止网络错误
        singletonWebSocket.sendBinaryMessage(data)
    }

    private fun connectInternal(url: String, deviceId: String, clientId: String, token: String) {
        singletonWebSocket.connect(
            url = url,
            deviceId = deviceId,
            clientId = clientId,
            token = token
        )
    }

    override fun startListening(mode: String) = protocol.startListening(mode)
    override fun stopListening() = protocol.stopListening()
    override fun sendText(text: String) = protocol.sendText(text)
    override fun abort(reason: String) = protocol.abort(reason)
}