package com.airobotcomm.tablet.airobotui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airobotcomm.tablet.comm.NetworkService
import com.airobotcomm.tablet.comm.NetworkState
import com.airobotcomm.tablet.comm.protocol.AiRobotEvent
import com.airobotcomm.tablet.airobotui.state.RobotState
import com.airobotcomm.tablet.airobotui.state.RobotStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.airobotcomm.tablet.system.SysManage
import com.airobotcomm.tablet.audio.AudioEvent
import com.airobotcomm.tablet.audio.AudioService
import com.airobotcomm.tablet.audio.AudioState
import com.airobotcomm.tablet.airobotui.state.ConversationSubState
import com.airobotcomm.tablet.system.SysState
import com.airobotcomm.tablet.system.model.AiAgent
import com.airobotcomm.tablet.system.model.DeviceInfo
import com.airobotcomm.tablet.system.model.SystemInfo

/**
 * 主控制 ViewModel
 * 负责机器人一级状态的管理与分发
 */
@HiltViewModel
class RobotMainViewModel @Inject constructor(
    private val networkService: NetworkService,
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

    private val _wakeupEvent = MutableSharedFlow<ByteArray?>(extraBufferCapacity = 1)
    val wakeupEvent: SharedFlow<ByteArray?> = _wakeupEvent.asSharedFlow()

    init {
        // step1: observe core states
        observeNetwork()
        observeSysState()
        observeAudioState()

        // start system
        sysManage.start()
    }

    /**
     * 初始化音频服务
     */
    fun initAudioService() {
        viewModelScope.launch {
            if (audioService.state.value is AudioState.Idle) {
                audioService.init()
            }
        }
    }

    private fun observeAudioState() {
        viewModelScope.launch {
            audioService.events.collect { event ->
                when (event) {
                    is AudioEvent.Wakeup -> handleWakeup(event)
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            audioService.state.collect { audioState ->
                val current = robotStateManager.robotState.value
                Log.d("RobotMainViewModel", "AudioState changed: $audioState, current RobotState: $current")
                // 仅在非异常状态下响应音频状态变化
                if (current is RobotState.Unauthorized || current is RobotState.Initializing || current is RobotState.Offline) {
                    return@collect
                }
                
                when (audioState) {
                    is AudioState.Active -> {
                        if (current !is RobotState.Conversation) {
                            Log.d("RobotMainViewModel", "Transitioning to Conversation(LISTENING) due to AudioState.Active")
                            robotStateManager.updateRobotState(RobotState.Conversation(ConversationSubState.LISTENING))
                        }
                    }
                    is AudioState.Waiting -> {
                        // 仅当处于 LISTENING 录制子状态，且底层回退到 Waiting 时，才视为对话中断/结束跳转到 Ready
                        if (current is RobotState.Conversation && current.subState == ConversationSubState.LISTENING) {
                            Log.d("RobotMainViewModel", "Transitioning to Ready due to AudioState.Waiting in LISTENING")
                            robotStateManager.updateRobotState(RobotState.Ready)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun handleWakeup(event: AudioEvent.Wakeup) {
        val currentState = robotStateManager.robotState.value
        // 只有在 Ready 状态或对话状态，且网络连接正常时才响应唤醒
        if ((currentState is RobotState.Ready || currentState is RobotState.Conversation) && 
            networkService.isConnected) {
            viewModelScope.launch {
                _wakeupEvent.emit(event.data)
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
                        networkService.connect()
                        // 尝试初始化音频服务 (配置可从 SystemInfo 获取，此处暂用默认)
                        // 注意：音频初始化可能需要权限，通常在 UI 层或确保权限后调用。
                        // 这里暂时不调用 audioService.init，由 ConversationViewModel 或 MainViewModel 的 UI Event 触发
                    }
                    is SysState.UpdateAvailable -> {
                        // TODO: 处理更新提示
                        robotStateManager.updateRobotState(RobotState.Ready)
                        // Also try to connect if update is available, assuming it functions
                        networkService.connect()
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
            networkService.state.collect { state ->
                when (state) {
                    NetworkState.CONNECTING, NetworkState.RECONNECTING -> {
                        robotStateManager.updateRobotState(RobotState.Connecting)
                    }
                    NetworkState.CONNECTED -> {
                        // 由 handleAiRobotEvent(AiRobotEvent.Connected) 处理
                    }
                    NetworkState.ERROR, NetworkState.IDLE -> {
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
            networkService.events.collect { event ->
                handleAiRobotEvent(event)
            }
        }
    }

    private fun handleAiRobotEvent(event: AiRobotEvent) {
        when (event) {
            is AiRobotEvent.Connected -> {
                // 仅在非对话状态下切回 Ready，防止覆盖对话状态
                if (robotStateManager.robotState.value !is RobotState.Conversation) {
                    Log.d("RobotMainViewModel", "Safe transitioning to Ready due to Connected event")
                    robotStateManager.updateRobotState(RobotState.Ready)
                }
                _errorMessage.value = null
            }
            is AiRobotEvent.Disconnected -> {
                robotStateManager.updateRobotState(RobotState.Offline)
            }
            is AiRobotEvent.Error -> {
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

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * 更新一级状态
     */
    fun updateRobotState(newState: RobotState) {
        robotStateManager.updateRobotState(newState)
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
        networkService.disconnect()
    }
}
