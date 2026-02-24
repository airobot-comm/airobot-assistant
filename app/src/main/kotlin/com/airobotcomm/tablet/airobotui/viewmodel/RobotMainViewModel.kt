package com.airobotcomm.tablet.airobotui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.airobotcomm.tablet.system.SysManage
import com.airobotcomm.tablet.system.SysState
import com.airobotcomm.tablet.system.model.AiAgent
import com.airobotcomm.tablet.system.model.DeviceInfo
import com.airobotcomm.tablet.system.model.SystemInfo
import com.airobotcomm.tablet.comm.NetCommService
import com.airobotcomm.tablet.comm.NetworkState
import com.airobotcomm.tablet.comm.NetCommEvent
import com.airobotcomm.tablet.airobotui.state.RobotState
import com.airobotcomm.tablet.airobotui.state.RobotStateManager
import com.airobotcomm.tablet.audio.AudioEvent
import com.airobotcomm.tablet.audio.AudioService

/**
 * 主控制 ViewModel
 * 负责机器人一级状态的管理与分发
 */
@HiltViewModel
class RobotMainViewModel @Inject constructor(
    private val netCommService: NetCommService,
    private val robotStateManager: RobotStateManager,
    private val sysManage: SysManage,
    private val audioService: AudioService
) : ViewModel() {

    val robotState: StateFlow<RobotState> = robotStateManager.robotState
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    private val _showActivationDialog = MutableStateFlow(false)
    val showActivationDialog: StateFlow<Boolean> = _showActivationDialog.asStateFlow()
    private val _activationCode = MutableStateFlow<String?>(null)
    val activationCode: StateFlow<String?> = _activationCode.asStateFlow()

    private val _voiceLevel = MutableStateFlow(0f)
    val voiceLevel: StateFlow<Float> = _voiceLevel.asStateFlow()

    private val _wakeupEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val wakeupEvent: SharedFlow<Unit> = _wakeupEvent.asSharedFlow()

    init {
        // step1: observe core states
        observeNetwork()
        observeSysState()
        observeAudioEvents()

        // start system
        sysManage.start()
    }

    /**
     * 初始化音频服务
     */
    fun initAudioService() {
        viewModelScope.launch {
            audioService.init()
       }
    }

    private fun observeAudioEvents() {
        viewModelScope.launch {
            audioService.events.collect { event ->
                when (event) {
                    is AudioEvent.Wakeup -> {
                        val currentState = robotStateManager.robotState.value
                        Log.d("RobotMainViewModel",
                            "Wakeup detected. Current state: $currentState")

                        // 核心安全逻辑：检查进入对话的先决条件，只有具备条件才真正启动对话
                        val isNetworkReady = netCommService.isConnected
                        val isSystemReady = sysManage.state.value is com.airobotcomm.tablet.system.SysState.Ready
                        if (isNetworkReady && isSystemReady &&
                            (currentState is RobotState.Ready || currentState is RobotState.Conversation)) {
                            Log.d("RobotMainViewModel", "Conditions met. Proceeding with wakeup.")
                            viewModelScope.launch {
                                _wakeupEvent.emit(Unit)
                            }
                        } else {
                            // 安全回退：不满足条件，强制将音频服务拉回 WAITING 状态，停止数据发送
                            Log.w("RobotMainViewModel", "Conditions NOT met (Net:$isNetworkReady, Sys:$isSystemReady). Pulling back Audio Service.")
                            audioService.deactivate()
                        }
                    }
                    is AudioEvent.VoiceLevel -> {
                        _voiceLevel.value = event.level
                    }
                    is AudioEvent.SystemError -> {
                        _errorMessage.value = event.message
                    }
                    else -> {}
                }
            }
        }
    }

    private fun observeSysState() {
        viewModelScope.launch {
            sysManage.state.collect { state ->
                when (state) {
                    is SysState.Checking -> {
                        robotStateManager.updateRobotState(RobotState.Initializing)
                    }
                    is SysState.DeviceActivationRequired -> {
                        robotStateManager.updateRobotState(
                            RobotState.Unauthorized("DEVICE_ACTIVATION"))
                        _showActivationDialog.value = false
                    }
                    is SysState.AiRobotActivationRequired -> {
                        val code = state.code
                        _activationCode.value = code
                        _showActivationDialog.value = true
                        robotStateManager.updateRobotState(
                            RobotState.Unauthorized(code))
                    }
                    is SysState.Ready -> {
                        _showActivationDialog.value = false
                        _activationCode.value = null
                        // OTA 激活/检查完成后，尝试连接网络
                        netCommService.connect()
                        // 尝试初始化音频服务 (配置可从 SystemInfo 获取，此处暂用默认)
                        // 注意：音频初始化可能需要权限，通常在 UI 层或确保权限后调用。
                        // 这里暂时不调用 audioService.init，由 ConversationViewModel 或 MainViewModel 的 UI Event 触发
                    }
                    is SysState.UpdateAvailable -> {
                        // TODO: 处理更新提示
                        robotStateManager.updateRobotState(RobotState.Ready)
                        // Also try to connect if update is available, assuming it functions
                        netCommService.connect()
                    }
                    is SysState.Error -> {
                        _errorMessage.value = state.message
                        robotStateManager.updateRobotState(RobotState.Offline)
                    }
                    is SysState.Idle -> {}
                }
            }
        }
    }

    private fun observeNetwork() {
        // 监听网络状态
        viewModelScope.launch {
            netCommService.state.collect { state ->
                when (state) {
                    NetworkState.CONNECTING, NetworkState.RECONNECTING -> {
                        robotStateManager.updateRobotState(RobotState.Connecting)
                    }
                    NetworkState.CONNECTED -> {
                        // 由 handleAiRobotEvent(NetCommEvent.Connected) 处理
                    }
                    NetworkState.ERROR, NetworkState.IDLE -> {
                        Log.w("RobotMainViewModel", "Network state is $state. Deactivating Audio Service.")
                        audioService.deactivate() // 强制回退音频服务

                        // 避免在已经 Unauthorized 的情况下切回 Offline，除非确实断开了
                        if (robotStateManager.robotState.value !is RobotState.Unauthorized && 
                            robotStateManager.robotState.value !is RobotState.Initializing) {
                            robotStateManager.updateRobotState(RobotState.Offline)
                        }
                    }
                }
            }
        }

        // 监听特定事件
        viewModelScope.launch {
            netCommService.events.collect { event ->
                handleAiRobotEvent(event)
            }
        }
    }

    private fun handleAiRobotEvent(event: NetCommEvent) {
        when (event) {
            is NetCommEvent.Connected -> {
                // 仅在非对话状态下切回 Ready，防止覆盖对话状态
                if (robotStateManager.robotState.value !is RobotState.Conversation) {
                    Log.d("RobotMainViewModel", "Safe transitioning to Ready due to Connected event")
                    robotStateManager.updateRobotState(RobotState.Ready)
                }
                _errorMessage.value = null
            }
            is NetCommEvent.Disconnected -> {
                Log.w("RobotMainViewModel", "Received Disconnected event. Deactivating Audio Service.")
                audioService.deactivate()
                robotStateManager.updateRobotState(RobotState.Offline)
            }
            is NetCommEvent.Error -> {
                Log.e("RobotMainViewModel", "Received Error event: ${event.message}. Deactivating Audio Service.")
                audioService.deactivate()
                _errorMessage.value = event.message
            }
            else -> {}
        }
    }

    fun onActivationConfirmed() {
        val code = _activationCode.value
        if (code != null) {
            viewModelScope.launch {
                sysManage.confirmAiRobotActivation(code)
            }
        }
    }

    fun activateDevice(productKey: String) {
        viewModelScope.launch {
            sysManage.deviceActivate(productKey)
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun configureAndActivateAiAgent(agentUrl: String, agentVender: String) {
        viewModelScope.launch {
            sysManage.configureAiAgent(agentUrl, agentVender)
                .onFailure { _errorMessage.value = it.message }
        }
    }

    // System Info Exposure (Reactive)
    val systemInfo: StateFlow<SystemInfo> = sysManage.systemInfo

    // Device Info Exposure (derived from hierarchical agentVendor)
    val deviceInfo = systemInfo.map { it.deviceInfo }
        .stateIn(viewModelScope, SharingStarted.Lazily, DeviceInfo.empty())
    
    val deviceActivation = deviceInfo.map { it.activation }
        .stateIn(viewModelScope, SharingStarted.Lazily, DeviceInfo.empty().activation)

    val isDeviceActivated = deviceActivation.map { it.isActivated }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    // AIRobot Info Exposure (derived from hierarchical agentVendor)
    val aiAgent = systemInfo.map { it.aiAgent }
        .stateIn(viewModelScope, SharingStarted.Lazily, AiAgent())

    val isAiRobotActivated = aiAgent.map { 
        it.activationCode.isNotEmpty() || it.commCredentials != null // sometime ai-agent hasn't activationCode
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    override fun onCleared() {
        super.onCleared()
        audioService.release()
        netCommService.disconnect()
    }
}
