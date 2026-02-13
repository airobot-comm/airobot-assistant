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
    
    // 会话激活标志，用于过滤已终止会话的延迟消息
    private var isActive = false

    init {
        // startEventListening for airobot-comm
        viewModelScope.launch {
            networkService.events.collect { event ->
                if (isActive || event is AiRobotEvent.Connected || event is AiRobotEvent.Disconnected) {
                    handleAiRobotEvent(event)
                }
            }
        }

        // startEventListening for audio service
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
                handleTtsAudioFrame(event.data)
            }
            is AiRobotEvent.DialogueEnd -> {
                handleDialogueEnd()
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
        if (current is RobotState.Ready || current is RobotState.Conversation
            || current is RobotState.Connecting) {
            robotStateManager.updateRobotState(
                            RobotState.Conversation(_subState.value))
        } else {
            Log.w(TAG, "syncSubState ignored because current state is $current")
        }
    }

    private fun addMessage(message: Message) {
        _messages.value = _messages.value + message
    }

    private fun handleSttResult(text: String) {
        if (text.isNotBlank()) {
            // 服务器处理反馈：有时候 ttsStart 会早于 STT 到达
            // 只要收到 STT，我们就确保它被记录并显示
            _currentRoundUserText.value = text
            addMessage(Message(role = MessageRole.USER, content = text))

            // 仅在 LISTENING 状态下才迁移到 THINKING
            // 如果已经是 SPEAKING，说明 TTS 已经开始，保持 SPEAKING 状态继续播报
            if (_subState.value == ConversationSubState.LISTENING) {
                Log.d(TAG, "STT received, transitioning LISTENING -> THINKING")
                _subState.value = ConversationSubState.THINKING
                syncSubState()
            } else {
                Log.d(TAG, "STT received during ${_subState.value}, text displayed but state unchanged")
            }
        }
    }

    private fun handleTtsStart() {
        Log.d(TAG, "TTS Start received, transitioning to SPEAKING")
        // 即便还没收到 STT (服务器并发处理延迟)，也先切到 SPEAKING，因为播报已经开始了
        _subState.value = ConversationSubState.SPEAKING
        syncSubState()
    }

    private fun handleTtsStop() {
        audioService.stopPlaying()
        viewModelScope.launch {
            // 延时一点时间让对话框展示更长时间
            delay(200)
            if (isAutoMode) {
                // 自动模式：继续下一轮
                startNextRound()
            } else {
                // 非自动模式：彻底清理对话内容并回退状态
                cleanConversation()
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

    private fun handleTtsAudioFrame(data: ByteArray) {
        // only speak when not muted and in speaking state
        if (!_isMuted.value && _subState.value == ConversationSubState.SPEAKING)
            audioService.play(data)
    }

    private fun handleDialogueEnd() {
        // 对话结束，回退并清除本次对话信息
        Log.d(TAG, "DialogueEnd received, deactivating audio")
        cleanConversation()
    }

    private fun handleAudioEvent(event: AudioEvent) {
        when (event) {
            is AudioEvent.SpeechData -> {
                // 只有在 LISTENING 状态且会话激活时才发送音频数据
                if (isActive && _subState.value == ConversationSubState.LISTENING) {
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
        isActive = true
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

    fun interrupt() {
        networkService.abort("user_interrupt")
        cleanConversation()
    }

    fun stopAutoConversation() {
        networkService.abort("stop_auto_mode")
        cleanConversation()
    }

    private fun resetRoundText() {
        _currentRoundUserText.value = null
        _currentRoundAiText.value = null
    }

    private fun cleanConversation(){
        isActive = false
        isAutoMode = false
        audioService.deactivate()
        audioService.stopPlaying()

        // clean conversation text
        resetRoundText()

        // 显式重置状态（是否必要？）
        _subState.value = ConversationSubState.LISTENING
        robotStateManager.updateRobotState(RobotState.Ready)
    }

    private fun startNextRound() {
        if (!isAutoMode || !networkService.isConnected) {
            isActive = false
            audioService.deactivate()
            return
        }
        isActive = true
        resetRoundText()
        _subState.value = ConversationSubState.LISTENING
        syncSubState()

        // 开启新轮次网络侦听
        networkService.startListening("auto")
        audioService.activate()
    }
}
