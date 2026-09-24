package com.airobot.assistant.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.airobot.core.system.SysManage
import com.airobot.core.system.SysState
import com.airobot.core.system.model.AiAgent
import com.airobot.core.system.model.DeviceInfo
import com.airobot.core.system.model.SystemInfo
import com.airobot.core.comm.NetCommService
import com.airobot.core.comm.NetworkState
import com.airobot.core.comm.NetCommEvent
import com.airobot.airbot.domain.model.RobotState
import com.airobot.airbot.api.AirbotEngineApi
import com.airobot.airbot.api.AirbotCharacterApi
import com.airobot.agent.audio.AudioEvent
import com.airobot.agent.audio.AudioService
import com.airobot.agent.brain.AiBrain
import com.airobot.agent.manager.AgentManager

/**
 * 主外壳控制 ViewModel
 * 协调网络通信与系统底层引擎之间的全局状态同步。
 */
@HiltViewModel
class MainShellViewModel @Inject constructor(
    private val netCommService: NetCommService,
    private val airbotEngineApi: AirbotEngineApi,
    private val airbotCharacterApi: AirbotCharacterApi,
    private val sysManage: SysManage,
    private val audioService: AudioService,
    private val aiBrain: AiBrain,
    private val agentManager: AgentManager
) : ViewModel() {

    val robotState: StateFlow<RobotState> = airbotEngineApi.robotState
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    private val _showActivationDialog = MutableStateFlow(false)
    val showActivationDialog: StateFlow<Boolean> = _showActivationDialog.asStateFlow()
    private val _activationCode = MutableStateFlow<String?>(null)
    val activationCode: StateFlow<String?> = _activationCode.asStateFlow()

    private val _wakeWordValidationResult = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val wakeWordValidationResult = _wakeWordValidationResult.asSharedFlow()

    private val _uiEvents = MutableSharedFlow<MainShellUiEvent>(extraBufferCapacity = 16)
    val uiEvents = _uiEvents.asSharedFlow()

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
     * 初始化语音输入服务
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
                        val currentState = airbotEngineApi.robotState.value
                        Log.d("MainShellViewModel",
                            "Wakeup detected. Current state: $currentState")

                        // 唤醒后的条件过滤：网络必须连接且系统就绪，且机器人处于 Ready/Conversation 状态时才执行唤醒确认（否则拉回 Audio 状态）
                        val isNetworkReady = netCommService.isConnected
                        val isSystemReady = sysManage.state.value is SysState.Ready
                        if (isNetworkReady && isSystemReady &&
                            (currentState is RobotState.Ready || currentState is RobotState.Conversation || currentState is RobotState.FunctionService)) {
                            Log.d("MainShellViewModel", "Conditions met. Proceeding with wakeup.")
                            viewModelScope.launch {
                                _wakeupEvent.emit(Unit)
                            }
                        } else {
                            // 条件不满足：可能是初始化中或未激活，此时不响应唤醒并重置音频服务到 WAITING 监听状态
                            Log.w("MainShellViewModel", "Conditions NOT met (Net:$isNetworkReady, Sys:$isSystemReady). Pulling back Audio Service.")
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
                        airbotEngineApi.updateEngineState(RobotState.Initializing)
                    }
                    is SysState.DeviceActivationRequired -> {
                        airbotEngineApi.updateEngineState(
                            RobotState.Unauthorized("DEVICE_ACTIVATION"))
                        _showActivationDialog.value = false
                    }
                    is SysState.AiRobotActivationRequired -> {
                        val code = state.code
                        _activationCode.value = code
                        _showActivationDialog.value = true
                        airbotEngineApi.updateEngineState(
                            RobotState.Unauthorized(code))
                    }
                    is SysState.Ready -> {
                        _showActivationDialog.value = false
                        _activationCode.value = null
                        // 系统就绪即开启网络连接（自动根据凭证进行 OTA 认证/激活）
                        netCommService.connect()
                        // 此时音频服务在系统层已由 SysManage 完成初始化。
                        // 外部在需要时，由 UI 触发或是 ConversationViewModel 层对 MainViewModel 的 UI Event 处理。
                    }
                    is SysState.UpdateAvailable -> {
                        // TODO: 处理可更新状态
                        airbotEngineApi.updateEngineState(RobotState.Ready)
                        // Also try to connect if update is available, assuming it functions
                        netCommService.connect()
                    }
                    is SysState.Error -> {
                        _errorMessage.value = state.message
                        airbotEngineApi.updateEngineState(RobotState.Offline)
                    }
                    is SysState.Idle -> {}
                }
            }
        }
    }

    private fun observeNetwork() {
        // 观察网络状态变更
        viewModelScope.launch {
            netCommService.state.collect { state ->
                when (state) {
                    NetworkState.CONNECTING, NetworkState.RECONNECTING -> {
                        airbotEngineApi.updateEngineState(RobotState.Connecting)
                    }
                    NetworkState.CONNECTED -> {
                        // 用 handleAiRobotEvent(NetCommEvent.Connected) 处理
                    }
                    NetworkState.ERROR, NetworkState.IDLE -> {
                        Log.w("MainShellViewModel", "Network state is $state. Deactivating Audio Service.")
                        audioService.deactivate() // 确保网络断开时停止录音传输

                        // 排除初始化和未授权状态，重置为 Offline
                        if (airbotEngineApi.robotState.value !is RobotState.Unauthorized &&
                            airbotEngineApi.robotState.value !is RobotState.Initializing) {
                            airbotEngineApi.updateEngineState(RobotState.Offline)
                        }
                    }
                }
            }
        }

        // 观察核心业务事件
        viewModelScope.launch {
            netCommService.events.collect { event ->
                handleAiRobotEvent(event)
            }
        }
    }

    private fun handleAiRobotEvent(event: NetCommEvent) {
        when (event) {
            is NetCommEvent.Connected -> {
                // 连接成功也可能在会话中，此时不应重置为 Ready（由服务端接管会话状态）
                if (airbotEngineApi.robotState.value !is RobotState.Conversation) {
                    Log.d("MainShellViewModel", "Safe transitioning to Ready due to Connected event")
                    airbotEngineApi.updateEngineState(RobotState.Ready)
                }
                _errorMessage.value = null
            }
            is NetCommEvent.Disconnected -> {
                Log.w("MainShellViewModel", "Received Disconnected event. Deactivating Audio Service.")
                audioService.deactivate()
                airbotEngineApi.updateEngineState(RobotState.Offline)
            }
            is NetCommEvent.Error -> {
                Log.e("MainShellViewModel", "Received Error event: ${event.message}. Deactivating Audio Service.")
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

    fun updateActiveRole(roleName: String) {
        viewModelScope.launch {
            airbotCharacterApi.switchCharacter(roleName)
        }
    }

    // System Info Exposure (Reactive)
    val systemInfo: StateFlow<SystemInfo> = sysManage.systemInfo
    val allCharacters = airbotCharacterApi.allCharacters
    val activeCharacter = airbotCharacterApi.activeCharacter

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

    fun updateActiveRoleIndex(index: Int) {
        viewModelScope.launch {
            val characters = allCharacters.value
            if (index in characters.indices) {
                val roleName = characters[index].roleName
                airbotCharacterApi.switchCharacter(roleName)
            }
        }
    }

    fun updateWakeWord(wakeWord: String) {
        viewModelScope.launch {
            val roleName = activeCharacter.value?.roleName ?: return@launch
            val success = airbotCharacterApi.updateWakeWord(roleName, wakeWord)
            _wakeWordValidationResult.emit(success)
            if (!success) {
                _uiEvents.tryEmit(MainShellUiEvent.ShowAlert(com.airobot.framework.R.string.invalid_wake_word))
            }
        }
    }

    val isSpeechInterruptionEnabled: StateFlow<Boolean> = agentManager.config
        .map { it.isSpeechInterruptionEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setSpeechInterruptionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            agentManager.setSpeechInterruptionEnabled(enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioService.release()
        netCommService.disconnect()
    }
}

/**
 * UI Event emitted by the MainShellViewModel to trigger side effects in the UI layer.
 */
sealed interface MainShellUiEvent {
    data class ShowAlert(val messageResId: Int) : MainShellUiEvent
}
