package com.airobotcomm.tablet.airobotui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.airobotcomm.tablet.audio.AudioEvent
import com.airobotcomm.tablet.audio.AudioService
import com.airobotcomm.tablet.audio.AudioState
import com.airobotcomm.tablet.system.model.Message
import com.airobotcomm.tablet.system.model.MessageRole
import com.airobotcomm.tablet.system.model.SystemInfo
import com.airobotcomm.tablet.comm.NetworkService
import com.airobotcomm.tablet.comm.NetworkState
import com.airobotcomm.tablet.comm.protocol.AiRobotEvent
import com.airobotcomm.tablet.airobotui.state.ConversationSubState
import com.airobotcomm.tablet.airobotui.state.RobotState
import com.airobotcomm.tablet.airobotui.state.RobotStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay

/**
 * 对话ViewModel
 */
@HiltViewModel
class ConversationViewModel @Inject constructor(
    application: Application,
    private val networkService: NetworkService,
    private val audioService: AudioService, // Use Interface
    private val robotStateManager: RobotStateManager
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "ConversationViewModel"
    }

    // 内部子状态管理
    private val _subState = MutableStateFlow(ConversationSubState.LISTENING)
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    private val _errorMessage = MutableStateFlow<String?>(null)

    // 当前轮次的用户输入文本
    private val _currentRoundUserText = MutableStateFlow<String?>(null)
    val currentRoundUserText: StateFlow<String?> = _currentRoundUserText.asStateFlow()
    
    // 当前轮次的AI回复文本
    private val _currentRoundAiText = MutableStateFlow<String?>(null)
    val currentRoundAiText: StateFlow<String?> = _currentRoundAiText.asStateFlow()

    // 静音状态管理
    private val _isMuted = MutableStateFlow(false)

    // 音频强度状态
    private val _audioLevel = MutableStateFlow(0.0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

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
        startEventListening()
        
        // 初始化音频管理器 (通常由 MainViewModel 或 Activity 保证权限后调用，这里作为保险或初始化入口)
        // 注意：如果 MainViewModel 已经做过，这里再次调用 init 也是安全的（AudioServiceImpl 中需处理重复 init）
        // 实际上建议在 Activity/Fragment 中请求权限成功后调用一次 init
        if (!audioService.init()) {
            _errorMessage.value = "音频系统初始化失败"
        }

        connectToServer()
    }
    
    private fun startEventListening() {
        viewModelScope.launch {
            networkService.events.collect { event ->
                handleAiRobotEvent(event)
            }
        }

        viewModelScope.launch {
            audioService.events.collect { event ->
                handleAudioEvent(event)
            }
        }
    }

    private fun handleAiRobotEvent(event: AiRobotEvent) {
        when (event) {
            is AiRobotEvent.Disconnected -> {
                audioService.deactivate()
                audioService.stopPlaying()
            }
            is AiRobotEvent.Error -> {
                _errorMessage.value = event.message
                // 错误时不强制重置 MainState，由 MainViewModel 处理
            }
            is AiRobotEvent.STT -> {
                handleSttResult(event.text)
            }
            is AiRobotEvent.TtsSentence -> {
                handleTtsSentence(event.text)
            }
            is AiRobotEvent.TtsStart -> {
                _subState.value = ConversationSubState.SPEAKING
                syncSubState()
            }
            is AiRobotEvent.TtsStop -> {
                handleTtsStop()
            }
            is AiRobotEvent.AudioFrame -> {
                if (!_isMuted.value) audioService.play(event.data)
            }
            is AiRobotEvent.DialogueEnd -> {
                // 对话结束，回退到 Waiting
                audioService.deactivate()
                audioService.stopPlaying()
                // RobotState update handled by MainViewModel observing AudioState
            }
            else -> {
                // network,wakeup event handled by MainViewModel
            }
        }
    }

    private fun syncSubState() {
        val current = robotStateManager.robotState.value
        // 只有在 Ready（允许开启对话）或已经在 Conversation 状态时，才同步子状态
        if (current is RobotState.Ready || current is RobotState.Conversation) {
            robotStateManager.updateRobotState(RobotState.Conversation(_subState.value))
        }
    }

    private fun handleSttResult(text: String) {
        if (text.isNotBlank()) {
            currentUserMessage = text
            _currentRoundUserText.value = text
            addMessage(Message(role = MessageRole.USER, content = text))
            
            _subState.value = ConversationSubState.THINKING
            syncSubState()
        }
    }

    private fun handleTtsSentence(text: String) {
        _currentRoundAiText.value = text
        addMessage(Message(role = MessageRole.ASSISTANT, content = text))
    }

    private fun handleTtsStop() {
        audioService.stopPlaying()
        viewModelScope.launch {
            delay(500)
            if (isAutoMode) {
                // 自动模式：继续下一轮
                startNextRound()
            } else {
                // 非自动模式：结束对话，回退状态
                audioService.deactivate()
            }
        }
    }

    private fun connectToServer() {
        networkService.connect()
    }

    private fun handleAudioEvent(event: AudioEvent) {
        when (event) {
            is AudioEvent.SpeechData -> {
                // 只有在 LISTENING 状态才发送音频数据
                if (_subState.value == ConversationSubState.LISTENING) {
                    networkService.sendAudio(event.data)
                }
            }
            is AudioEvent.VoiceLevel -> {
                _audioLevel.value = event.level
            }
            is AudioEvent.Wakeup -> {
                Log.d(TAG, "KWS 唤醒触发 (ConversationContext)")
                isAutoMode = true
                resetRoundText()
                _subState.value = ConversationSubState.LISTENING
                syncSubState()
                
                // 通知服务端启动会话侦听(上下文音频需紧随其后)
                networkService.startListening("auto")
                
                // 上下文音频发送
                event.data?.let { networkService.sendAudio(it) }
            }
            is AudioEvent.SystemError -> {
                Log.e(TAG, "音频错误: ${event.message}")
                _errorMessage.value = event.message
            }
            else -> {}
        }
    }

    fun startAutoConversation() {
        if (!networkService.isConnected) return
        isAutoMode = true
        resetRoundText()
        _subState.value = ConversationSubState.LISTENING
        syncSubState()
        
        // 1. 激活网络侦听
        networkService.startListening("auto")
        
        // 2. 激活音频硬件
        audioService.activate()
    }

    private fun startNextRound() {
        if (!isAutoMode || !networkService.isConnected) {
            audioService.deactivate()
            return
        }
        resetRoundText()
        _subState.value = ConversationSubState.LISTENING
        syncSubState()
        
        // 开启新轮次网络侦听
        networkService.startListening("auto")
        
        audioService.activate()
    }

    fun interrupt() {
        audioService.stopPlaying()
        audioService.deactivate()
        networkService.abort("user_interrupt")
        isAutoMode = false
    }

    fun stopAutoConversation() {
        isAutoMode = false
        audioService.deactivate()
        audioService.stopPlaying()
        networkService.abort("stop_auto_mode")
    }

    private fun resetRoundText() {
        currentUserMessage = null
        _currentRoundUserText.value = null
        _currentRoundAiText.value = null
    }

    private fun addMessage(message: Message) {
        _messages.value = _messages.value + message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        if (_isMuted.value) {
            audioService.stopPlaying()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioService.release()
        networkService.disconnect()
    }
}
