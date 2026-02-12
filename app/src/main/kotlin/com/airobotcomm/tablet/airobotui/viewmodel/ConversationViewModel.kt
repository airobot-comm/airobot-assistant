package com.airobotcomm.tablet.airobotui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.airobotcomm.tablet.audio.AudioEvent
import com.airobotcomm.tablet.audio.AudioService
import com.airobotcomm.tablet.system.model.Message
import com.airobotcomm.tablet.system.model.MessageRole
import com.airobotcomm.tablet.comm.NetworkService
import com.airobotcomm.tablet.comm.protocol.AiRobotEvent
import com.airobotcomm.tablet.airobotui.state.ConversationSubState
import com.airobotcomm.tablet.airobotui.state.RobotState
import com.airobotcomm.tablet.airobotui.state.RobotStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay

/**
 * 对话ViewModel，处理conversation 状态，事件，发起对话与终止对话
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
        startEventListening()
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
            is AiRobotEvent.STT -> {
                handleSttResult(event.text)
            }
            is AiRobotEvent.TtsStart -> {
                handleTtsStart()
            }
            is AiRobotEvent.TtsStop -> {
                handleTtsStop()
            }
            is AiRobotEvent.TtsSentence -> {
                handleTtsSentence(event.text)
            }
            is AiRobotEvent.AudioFrame -> {
                if (!_isMuted.value) audioService.play(event.data)
            }
            is AiRobotEvent.DialogueEnd -> {
                // 对话结束，回退到 Waiting
                Log.d(TAG, "DialogueEnd received, deactivating audio")
                audioService.deactivate()
                audioService.stopPlaying()
                // RobotState update handled by MainViewModel observing AudioState
            }
            else -> {
                // network error/connect，disconnect event handled by MainViewModel
            }
        }
    }

    private fun syncSubState() {
        val current = robotStateManager.robotState.value
        Log.d(TAG, "syncSubState: current=$current, target subState=${_subState.value}")
        // 允许在 Ready, Conversation 或 Connecting 状态下进行同步
        if (current is RobotState.Ready || current is RobotState.Conversation || current is RobotState.Connecting) {
            robotStateManager.updateRobotState(RobotState.Conversation(_subState.value))
        } else {
            Log.w(TAG, "syncSubState ignored because current state is $current")
        }
    }

    private fun handleSttResult(text: String) {
        if (text.isNotBlank()) {
            currentUserMessage = text
            _currentRoundUserText.value = text
            addMessage(Message(role = MessageRole.USER, content = text))
            
            // 仅在 LISTENING 状态下才迁移到 THINKING
            // 如果已经是 SPEAKING，说明 TTS 已经开始，不能回退到 THINKING
            if (_subState.value == ConversationSubState.LISTENING) {
                Log.d(TAG, "STT received, transitioning LISTENING -> THINKING")
                _subState.value = ConversationSubState.THINKING
                syncSubState()
            } else {
                Log.d(TAG, "STT received but ignored state change because current subState is ${_subState.value}")
            }
        }
    }

    private fun handleTtsStart() {
        Log.d(TAG, "TTS Start received, transitioning to SPEAKING")
        _subState.value = ConversationSubState.SPEAKING
        syncSubState()
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

    private fun handleTtsSentence(text: String) {
        _currentRoundAiText.value = text
        addMessage(Message(role = MessageRole.ASSISTANT, content = text))

        // 兜底：如果收到句子但还没切到 SPEAKING，补切一下
        if (_subState.value != ConversationSubState.SPEAKING) {
            Log.d(TAG, "TtsSentence received, ensuring state is SPEAKING")
            _subState.value = ConversationSubState.SPEAKING
            syncSubState()
        }
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

            is AudioEvent.SystemError -> {
                Log.e(TAG, "音频错误: ${event.message}")
                _errorMessage.value = event.message
            }
            else -> {
                // wakeup handled by mainViewModel
            }
        }
    }

    /**
     * 开启对话（AI 触发或手动触发）
     * @param contextData 唤醒时的上下文音频数据（可选）
     */
    fun startConversation(contextData: ByteArray? = null) {
        if (!networkService.isConnected) return
        isAutoMode = true
        resetRoundText()
        _subState.value = ConversationSubState.LISTENING
        syncSubState()
        
        // 1. 激活网络侦听
        networkService.startListening("auto")
        
        // 2. 如果有上下文音频，发送之
        contextData?.let { networkService.sendAudio(it) }
        
        // 3. 激活音频硬件
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

}
