package com.airobotcomm.tablet.commhub

import android.util.Log
import com.airobotcomm.tablet.domain.config.ConfigManager
import com.airobotcomm.tablet.commhub.protocol.AiRobotEvent
import com.airobotcomm.tablet.commhub.protocol.AiRobotProtocol
import com.airobotcomm.tablet.commhub.protocol.OtaService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.airobotcomm.tablet.commhub.protocol.ProtocolAdapter
import com.airobotcomm.tablet.commhub.transport.ConnectivityMonitor
import com.airobotcomm.tablet.commhub.transport.WebSocketEvent
import com.airobotcomm.tablet.commhub.transport.SingletonWebSocket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkServiceImpl @Inject constructor(
    private val otaService: OtaService,
    private val singletonWebSocket: SingletonWebSocket,
    private val configManager: ConfigManager,
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
                        val config = configManager.loadConfig()
                        protocol.open("", config.macAddress, config.token)
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
        val config = configManager.loadConfig()
        if (config.otaUrl.isBlank()) {
            _state.value = NetworkState.ERROR
            scope.launch { _events.emit(AiRobotEvent.Error("OTA URL is empty")) }
            return
        }

        _state.value = NetworkState.INITIALIZING
        scope.launch {
            try {
                // 1. 执行 OTA 获取 WS URL (兼具报备功能)
                val result = otaService.reportDeviceAndGetOta(
                    clientId = config.uuid,
                    deviceId = config.macAddress,
                    otaUrl = config.otaUrl
                )

                result.onSuccess { otaResponse ->
                    Log.d(TAG, "OTA Success")
                    
                    // 检查激活信息
                    otaResponse.activation?.let { activation ->
                        if (activation.code.isNotEmpty()) {
                            Log.d(TAG, "Device needs activation: ${activation.code}")
                            _events.emit(AiRobotEvent.ActivationRequired(activation.code))
                            _state.value = NetworkState.IDLE
                            return@onSuccess
                        }
                    }

                    Log.d(TAG, "Connecting to WS: ${otaResponse.websocket.url}")
                    _state.value = NetworkState.CONNECTING
                    
                    // 2. 连接 WebSocket (继续之前的逻辑)
                    connectInternal(otaResponse.websocket.url, config.macAddress, config.token)
                }.onFailure { e ->
                    Log.e(TAG, "OTA Failed", e)
                    _state.value = NetworkState.ERROR
                    _events.emit(AiRobotEvent.Error("Initialization failed: ${e.message}"))
                }
            } catch (e: Exception) {
                _state.value = NetworkState.ERROR
                _events.emit(AiRobotEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }

    override fun disconnect() {
        singletonWebSocket.close()
        _state.value = NetworkState.IDLE
    }

    override fun onActivationConfirmed() {
        Log.d(TAG, "Activation confirmed by user, retrying connect")
        connect()
    }

    override fun sendAudio(data: ByteArray) {
        Log.d(TAG, "发送二进制消息，长度: ${data.size}")
        // todo 发送语音数据需要优化，通过VAD检测减少发送数据，并做cache排队防止网络错误
        singletonWebSocket.sendBinaryMessage(data)
    }

    private fun connectInternal(url: String, deviceId: String, token: String) {
        singletonWebSocket.connect(
            url = url,
            deviceId = deviceId,
            token = token
        )
    }

    override fun startListening(mode: String) = protocol.startListening(mode)
    override fun stopListening() = protocol.stopListening()
    override fun sendText(text: String) = protocol.sendText(text)
    override fun abort(reason: String) = protocol.abort(reason)
}
