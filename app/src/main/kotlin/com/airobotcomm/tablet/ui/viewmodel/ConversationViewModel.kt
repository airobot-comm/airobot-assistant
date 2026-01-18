package com.airobotcomm.tablet.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.airobotcomm.tablet.audio.AudioEvent
import com.airobotcomm.tablet.audio.EnhancedAudioManager
import com.airobotcomm.tablet.data.ConfigManager
import com.airobotcomm.tablet.data.Message
import com.airobotcomm.tablet.data.MessageRole
import com.airobotcomm.tablet.data.DeviceConfig
import com.airobotcomm.tablet.network.NetworkService
import com.airobotcomm.tablet.network.NetworkState
import com.airobotcomm.tablet.network.protocol.AiRobotEvent
import com.airobotcomm.tablet.ui.state.ConversationSubState
import com.airobotcomm.tablet.ui.state.RobotState
import com.airobotcomm.tablet.ui.state.RobotStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay

// 移除旧的 enum class ConversationState

/**
 * 对话ViewModel
 */
@HiltViewModel
class ConversationViewModel @Inject constructor(
    application: Application,
    private val networkService: NetworkService,
    private val configManager: ConfigManager,
    private val audioManager: EnhancedAudioManager,
    private val robotStateManager: RobotStateManager // 使用 RobotStateManager 替代 MainViewModel
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "ConversationViewModel"
    }

    private val gson = Gson()

    // 内部子状态管理
    private val _subState = MutableStateFlow(ConversationSubState.LISTENING)
    val subState: StateFlow<ConversationSubState> = _subState.asStateFlow()

    // 移除旧的 _state 和 _isConnected，由 MainViewModel 管理

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // 激活弹窗状态
    // 激活弹窗状态
    private val _showActivationDialog = MutableStateFlow(false)
    val showActivationDialog: StateFlow<Boolean> = _showActivationDialog.asStateFlow()
    
    private val _activationCode = MutableStateFlow<String?>(null)
    val activationCode: StateFlow<String?> = _activationCode.asStateFlow()
    
    // 当前轮次的用户输入文本 (用于UI显示，每轮对话开始时清空)
    private val _currentRoundUserText = MutableStateFlow<String?>(null)
    val currentRoundUserText: StateFlow<String?> = _currentRoundUserText.asStateFlow()
    
    // 当前轮次的AI回复文本 (用于UI显示，确保换轮时清空)
    private val _currentRoundAiText = MutableStateFlow<String?>(null)
    val currentRoundAiText: StateFlow<String?> = _currentRoundAiText.asStateFlow()

    // 静音状态管理
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()
    
    // 音频强度状态 - 用于驱动波形动画
    private val _audioLevel = MutableStateFlow(0.0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    // 配置管理
    
    // 多轮对话支持
    private var isAutoMode = false
    private var currentUserMessage: String? = null

    init {
        initializeServices()
    }

    /**
     * 初始化服务
     */
    @SuppressLint("MissingPermission")
    private fun initializeServices() {
        // 首先启动事件监听，确保不会错过任何事件
        startEventListening()
        
        // 初始化音频管理器
        if (!audioManager.initialize()) {
            _errorMessage.value = "音频系统初始化失败"
            return
        }

        // 开始连接
        connectToServer()
    }
    
    /**
     * 启动事件监听
     */
    private fun startEventListening() {
        // 监听统一网络服务事件
        viewModelScope.launch {
            Log.d(TAG, "开始监听 NetworkService 事件")
            networkService.events.collect { event ->
                handleAiRobotEvent(event)
            }
        }

        // 监听网络连接状态
        viewModelScope.launch {
            networkService.state.collect { networkState ->
                updateStateFromNetwork(networkState)
            }
        }

        // 监听音频事件
        viewModelScope.launch {
            audioManager.audioEvents.collect { event ->
                handleAudioEvent(event)
            }
        }
    }

    /**
     * 根据网络服务状态映射 UI 状态
     */
    private fun updateStateFromNetwork(networkState: NetworkState) {
        // 一级状态由 MainViewModel 处理
    }

    /**
     * 处理统一的 AiRobot 协议事件
     */
    private fun handleAiRobotEvent(event: AiRobotEvent) {
        when (event) {
            is AiRobotEvent.ActivationRequired -> {
                // 由 MainViewModel 处理
            }
            is AiRobotEvent.Connected -> {
                // 由 MainViewModel 处理
                _errorMessage.value = null
            }
            is AiRobotEvent.Disconnected -> {
                // 由 MainViewModel 处理
                audioManager.stopRecording()
                audioManager.stopPlaying()
            }
            is AiRobotEvent.Error -> {
                _errorMessage.value = event.message
                robotStateManager.updateRobotState(RobotState.Ready)
            }
            is AiRobotEvent.STT -> {
                handleSttResult(event.text)
            }
            is AiRobotEvent.TtsSentence -> {
                handleTtsSentence(event.text)
            }
            is AiRobotEvent.TtsStart -> {
                _subState.value = ConversationSubState.SPEAKING
                syncToMainState()
            }
            is AiRobotEvent.TtsStop -> {
                handleTtsStop()
            }
            is AiRobotEvent.AudioFrame -> {
                if (!_isMuted.value) audioManager.playAudio(event.data)
            }
            is AiRobotEvent.DialogueEnd -> {
                robotStateManager.updateRobotState(RobotState.Ready)
                audioManager.stopRecording()
                audioManager.stopPlaying()
            }
            else -> {}
        }
    }

    private fun syncToMainState() {
        robotStateManager.updateRobotState(RobotState.Conversation(_subState.value))
    }

    private fun handleSttResult(text: String) {
        if (text.isNotBlank()) {
            currentUserMessage = text
            _currentRoundUserText.value = text
            addMessage(Message(role = MessageRole.USER, content = text))
            audioManager.stopRecording()
            _subState.value = ConversationSubState.THINKING
            syncToMainState()
        }
    }

    private fun handleTtsSentence(text: String) {
        _currentRoundAiText.value = text
        addMessage(Message(role = MessageRole.ASSISTANT, content = text))
    }

    private fun handleTtsStop() {
        audioManager.stopPlaying()
        viewModelScope.launch {
            delay(500)
            if (isAutoMode) {
                startNextRound()
            } else {
                robotStateManager.updateRobotState(RobotState.Ready)
            }
        }
    }

    /**
     * 连接到服务器（现在只需调用统一接口）
     */
    private fun connectToServer() {
        networkService.connect()
    }

    /**
     * 更新配置
     */
    fun updateConfig(newConfig: DeviceConfig) {
        configManager.saveConfig(newConfig)
        networkService.disconnect()
        robotStateManager.updateRobotState(RobotState.Offline)
        networkService.connect()
    }

    /**
     * 处理音频事件
     */
    private fun handleAudioEvent(event: AudioEvent) {
        when (event) {
            is AudioEvent.AudioData -> {
                // 只有在聆听状态才发送音频数据
                if (_subState.value == ConversationSubState.LISTENING) {
                    networkService.sendAudio(event.data)
                }
            }
            is AudioEvent.AudioLevel -> {
                // 更新音频强度状态
                _audioLevel.value = event.level
            }
            is AudioEvent.Error -> {
                Log.e(TAG, "音频错误: ${event.message}")
                _errorMessage.value = event.message
                stopListening()
            }
        }
    }

    /**
     * 用户确认激活
     */
    fun onActivationConfirmed() {
        _showActivationDialog.value = false
        _activationCode.value = null
        networkService.onActivationConfirmed()
    }

    /**
     * 开始聆听（手动模式）
     */
    fun startListening() {
        if (!networkService.isConnected) return
        isAutoMode = false
        resetRoundText()
        _subState.value = ConversationSubState.LISTENING
        syncToMainState()
        audioManager.startRecording()
        networkService.startListening("manual")
    }

    /**
     * 开始自动对话模式
     */
    fun startAutoConversation() {
        if (!networkService.isConnected) return
        isAutoMode = true
        resetRoundText()
        _subState.value = ConversationSubState.LISTENING
        syncToMainState()
        audioManager.startRecording()
        networkService.startListening("auto")
    }

    /**
     * 停止聆听
     */
    fun stopListening() {
        audioManager.stopRecording()
        _subState.value = ConversationSubState.THINKING
        syncToMainState()
        networkService.stopListening()
    }

    /**
     * 取消当前录音并发送中止信号
     */
    fun cancelListeningWithAbort(reason: String = "user_interrupt") {
        audioManager.stopRecording()
        networkService.abort(reason)
        robotStateManager.updateRobotState(RobotState.Ready)
    }

    /**
     * 开始下一轮对话
     */
    private fun startNextRound() {
        if (!isAutoMode || !networkService.isConnected) {
            robotStateManager.updateRobotState(RobotState.Ready)
            return
        }
        resetRoundText()
        _subState.value = ConversationSubState.LISTENING
        syncToMainState()
        audioManager.startRecording()
        networkService.startListening("auto")
    }

    /**
     * 发送文本消息
     */
    fun sendTextMessage(text: String) {
        if (!networkService.isConnected || text.isBlank()) return
        networkService.sendText(text)
        _subState.value = ConversationSubState.THINKING
        syncToMainState()
    }

    /**
     * 打断当前对话
     */
    fun interrupt() {
        audioManager.stopPlaying()
        audioManager.stopRecording()
        networkService.abort("user_interrupt")
        isAutoMode = false
        robotStateManager.updateRobotState(RobotState.Ready)
    }

    /**
     * 停止自动对话模式
     */
    fun stopAutoConversation() {
        isAutoMode = false
        audioManager.stopRecording()
        audioManager.stopPlaying()
        networkService.abort("stop_auto_mode")
        robotStateManager.updateRobotState(RobotState.Ready)
    }

    private fun resetRoundText() {
        currentUserMessage = null
        _currentRoundUserText.value = null
        _currentRoundAiText.value = null
    }

    /**
     * 添加消息到列表
     */
    private fun addMessage(message: Message) {
        _messages.value = _messages.value + message
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * 清除对话历史
     */
    fun clearMessages() {
        _messages.value = emptyList()
    }

    /**
     * 测试音频播放
     */
    fun testAudioPlayback() {
        audioManager.testAudioPlayback()
    }

    /**
     * 切换静音状态
     */
    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        Log.d(TAG, "静音状态切换为: ${_isMuted.value}")
        
        // 如果切换到静音状态，停止当前播放
        if (_isMuted.value) {
            audioManager.stopPlaying()
        }
    }

    /**
     * 处理MCP消息
     */
    private fun handleMCPMessage(message: String) {
        Log.d(TAG, "收到MCP消息: $message")
        
        // 添加MCP消息到对话列表（可选）
        addMessage(Message(
            role = MessageRole.SYSTEM,
            content = "MCP: $message"
        ))
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.cleanup()
        networkService.disconnect()
    }
}