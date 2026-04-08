package com.airobot.core.comm

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.airobot.core.comm.provider.CommSysProvider
import com.airobot.core.comm.protocol.CommProtocol
import com.airobot.core.comm.protocol.ProtocolAdapter
import com.airobot.core.comm.transport.ConnectivityMonitor
import com.airobot.core.comm.transport.WebSocketEvent
import com.airobot.core.comm.transport.SingletonWebSocket

@Singleton
class NetCommServiceImpl @Inject constructor(
    private val singletonWebSocket: SingletonWebSocket,
    private val sysProvider: CommSysProvider,
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
        // 閰嶇疆鍗忚灞傜殑鍙戦€佸洖璋?
        protocol.setRawSender { text ->
            singletonWebSocket.sendTextMessage(text)
        }

        // 妗ユ帴浼犺緭灞備簨浠?
        scope.launch {
            singletonWebSocket.events.collect { wsEvent ->
                when (wsEvent) {
                    is WebSocketEvent.Connected -> {
                        _state.value = NetworkState.CONNECTING // 浼犺緭灞?OK锛岃繘鍏ュ崗璁彙鎵?
                        scope.launch {
                            val deviceInfo = sysProvider.getDeviceInfo()
                            val credentials = sysProvider.getCommCredentials()
                            if (credentials != null) {
                                protocol.open("", deviceInfo.macAddress, credentials.token)
                            } else {
                                Log.e(TAG, "Connection successful but no credentials found")
                            }
                        }
                    }
                    is WebSocketEvent.Reconnecting -> {
                        _state.value = NetworkState.RECONNECTING
                        protocol.close()
                        _events.emit(NetCommEvent.Disconnected)
                    }
                    is WebSocketEvent.TextMessage -> {
                        // 1. 浜ょ粰鍗忚灞傚鐞嗘彙鎵嬬瓑鎺у埗閫昏緫
                        protocol.handleRawText(wsEvent.message)
                        // 2. 浜ょ粰閫傞厤鍣ㄨВ鏋愪笟鍔℃暟鎹?
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

        // 妗ユ帴鍗忚灞傞珮灞備簨浠?
        scope.launch {
            protocol.events.collect { protocolEvent ->
                if (protocolEvent is NetCommEvent.Connected) {
                    _state.value = NetworkState.CONNECTED
                }
                _events.emit(protocolEvent)
            }
        }

        // 鐩戝惉绯荤粺缃戠粶鍙樺寲骞跺皾璇曡嚜鍔ㄦ仮澶?
        scope.launch {
            connectivityMonitor.isNetworkAvailable.collect { isAvailable ->
                if (isAvailable && !isConnected && _state.value != NetworkState.IDLE) {
                    Log.d(TAG, "Network restored, reconnecting...")
                    connect()
                }
            }
        }
    }

    override fun connect() {
        scope.launch {
            if (!sysProvider.isDeviceActivated()) {
                _state.value = NetworkState.ERROR
                _events.emit(NetCommEvent.Error("Device not activated, please authenticate"))
                return@launch
            }

            // 2. Check AIRobot activation
            if (!sysProvider.isAiRobotActivated()) {
                _state.value = NetworkState.ERROR
                _events.emit(NetCommEvent.Error("AIRobot not activated, please activate in settings"))
                return@launch
            }

            // 3. Get composed parameters
            val deviceInfo = sysProvider.getDeviceInfo()
            val credentials = sysProvider.getCommCredentials()
            
            if (credentials == null || credentials.url.isBlank()) {
                _state.value = NetworkState.ERROR
                _events.emit(NetCommEvent.Error("Failed to get credentials"))
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
                _events.emit(NetCommEvent.Error(e.message ?: "Connection Failed"))
            }
        }
    }

    override fun disconnect() {
        singletonWebSocket.close()
        _state.value = NetworkState.IDLE
    }


    override fun sendAudio(data: ByteArray) {
        Log.d(TAG, "Sent binary msg, length: ${data.size}")
        // todo VAD cache...
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

