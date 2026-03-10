package com.airobot.assistant.comm

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.airobot.assistant.system.SysManage
import com.airobot.assistant.comm.protocol.CommProtocol
import com.airobot.assistant.comm.protocol.ProtocolAdapter
import com.airobot.assistant.comm.transport.ConnectivityMonitor
import com.airobot.assistant.comm.transport.WebSocketEvent
import com.airobot.assistant.comm.transport.SingletonWebSocket

@Singleton
class NetCommServiceImpl @Inject constructor(
    private val singletonWebSocket: SingletonWebSocket,
    private val sysManage: SysManage,
    private val protocolAdapter: ProtocolAdapter,
    private val connectivityMonitor: ConnectivityMonitor,
    private val protocol: CommProtocol
) : NetCommService {

    companion object {
        private const val TAG = "NetCommService"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _state = MutableStateFlow(NetworkState.IDLE)
    override val state: StateFlow<NetworkState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<NetCommEvent>(replay = 0)
    override val events: SharedFlow<NetCommEvent> = _events.asSharedFlow()

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
                        scope.launch {
                            val deviceInfo = sysManage.getDeviceInfo()
                            val credentials = sysManage.getCommCredentials()
                            if (credentials != null) {
                                protocol.open("", deviceInfo.macAddress, credentials.token)
                            } else {
                                Log.e(TAG, "连接成功但未找到凭证")
                            }
                        }
                    }
                    is WebSocketEvent.Reconnecting -> {
                        _state.value = NetworkState.RECONNECTING
                        protocol.close()
                        _events.emit(NetCommEvent.Disconnected)
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
                        _events.emit(NetCommEvent.AudioFrame(wsEvent.data))
                    }
                }
            }
        }

        // 桥接协议层高层事件
        scope.launch {
            protocol.events.collect { protocolEvent ->
                if (protocolEvent is NetCommEvent.Connected) {
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
            // 1. Check device activation
            if (!sysManage.isDeviceActivated()) {
                _state.value = NetworkState.ERROR
                _events.emit(NetCommEvent.Error("设备未激活，请先进行系统认证"))
                return@launch
            }

            // 2. Check AIRobot activation
            if (!sysManage.isAiRobotActivated()) {
                _state.value = NetworkState.ERROR
                _events.emit(NetCommEvent.Error("智能体未激活，请先在配置页完成激活"))
                return@launch
            }

            // 3. Get composed parameters
            val deviceInfo = sysManage.getDeviceInfo()
            val credentials = sysManage.getCommCredentials()
            
            if (credentials == null || credentials.url.isBlank()) {
                _state.value = NetworkState.ERROR
                _events.emit(NetCommEvent.Error("获取通信凭证失败，请重试"))
                return@launch
            }

            _state.value = NetworkState.CONNECTING
            try {
                connectInternal(
                    url = credentials.url,
                    macAddr = deviceInfo.macAddress,
                    clientId = credentials.clientId.ifEmpty { deviceInfo.deviceId },
                    token = credentials.token
                )
            } catch (e: Exception) {
                _state.value = NetworkState.ERROR
                _events.emit(NetCommEvent.Error(e.message ?: "连接失败"))
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

    /* convert clientId to macAddr because websocket need macAddr as deviceId
     * todo: add adapt-lay to resolve mac-client problem
     */
    private fun connectInternal(url: String, macAddr: String, clientId: String, token: String) {
        singletonWebSocket.connect(
            url = url,
            deviceId = macAddr,
            clientId = clientId,
            token = token
        )
    }

    override fun startListening(mode: String) = protocol.startListening(mode)
    override fun stopListening() = protocol.stopListening()
    override fun sendText(text: String) = protocol.sendText(text)
    override fun abort(reason: String) = protocol.abort(reason)
}
