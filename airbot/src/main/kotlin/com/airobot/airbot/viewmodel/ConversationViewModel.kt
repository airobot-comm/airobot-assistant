package com.airobot.airbot.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.airobot.audio.AudioEvent
import com.airobot.audio.AudioService
import com.airobot.airbot.state.ConversationSubState
import com.airobot.airbot.dialogue.Message
import com.airobot.airbot.dialogue.MessageRole
import com.airobot.airbot.state.RobotEngineState
import com.airobot.airbot.state.RobotStateEngine
import com.airobot.core.comm.NetCommEvent
import com.airobot.core.comm.NetCommService

/**
 * AI Conversation ViewModel
 */
@HiltViewModel
class ConversationViewModel @Inject constructor(
    application: Application,
    private val netCommService: NetCommService,
    private val audioService: AudioService,
    private val robotStateEngine: RobotStateEngine
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "ConversationViewModel"
    }

    private var isAutoMode = false
    private var isActive = false
    private val _subState = MutableStateFlow(ConversationSubState.LISTENING)
    private val _isMuted = MutableStateFlow(false)

    private val _messages = MutableStateFlow<List<Message>>(emptyList())

    private val _currentRoundUserText = MutableStateFlow<String?>(null)
    val currentRoundUserText: StateFlow<String?> = _currentRoundUserText.asStateFlow()

    private val _currentRoundAiText = MutableStateFlow<String?>(null)
    val currentRoundAiText: StateFlow<String?> = _currentRoundAiText.asStateFlow()

    private val _audioLevel = MutableStateFlow(0.0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    init {
        viewModelScope.launch {
            netCommService.events.collect { event ->
                if (isActive){
                    handleAiRobotCommEvent(event)
                }
            }
        }

        viewModelScope.launch {
            audioService.events.collect { event ->
                if (isActive && _subState.value == ConversationSubState.LISTENING){
                    handleAudioEvent(event)
                }
            }
        }
    }

    private fun handleAiRobotCommEvent(event: NetCommEvent) {
        when (event) {
            is NetCommEvent.STT -> handleSttResult(event.text)
            is NetCommEvent.TtsStart -> handleTtsStart()
            is NetCommEvent.TtsStop -> handleTtsStop()
            is NetCommEvent.TtsSentence -> handleTtsSentence(event.text)
            is NetCommEvent.AudioFrame -> handleTtsAudioFrame(event.data)
            is NetCommEvent.DialogueEnd -> handleDialogueEnd()
            else -> {}
        }
    }

    private fun handleAudioEvent(event: AudioEvent) {
        when (event) {
            is AudioEvent.SpeechData -> netCommService.sendAudio(event.data)
            is AudioEvent.VoiceLevel -> _audioLevel.value = event.level
            else -> return
        }
    }

    private fun syncSubState() {
        val current = robotStateEngine.robotEngineState.value
        Log.d(TAG, "syncSubState: current=$current, target subState=${_subState.value}")
        if (current is RobotEngineState.Ready || current is RobotEngineState.Conversation
            || current is RobotEngineState.Connecting) {
            robotStateEngine.updateEngineState(
                            RobotEngineState.Conversation(_subState.value))
        } else {
            Log.w(TAG, "syncSubState ignored because current state is $current")
        }
    }

    private fun addMessage(message: Message) {
        _messages.value = _messages.value + message
    }

    private fun handleSttResult(text: String) {
        if (text.isNotBlank()) {
            _currentRoundUserText.value = text
            addMessage(Message(role = MessageRole.USER, content = text))

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
        _subState.value = ConversationSubState.SPEAKING
        syncSubState()
    }

    private fun handleTtsStop() {
        audioService.stopPlaying()
        viewModelScope.launch {
            delay(200)
            if (isAutoMode) {
                startNextRound()
            } else {
                cleanConversation()
            }
        }
    }

    private fun handleTtsSentence(text: String) {
        _currentRoundAiText.value = text
        addMessage(Message(role = MessageRole.AGENT, content = text))

        if (_subState.value != ConversationSubState.SPEAKING) {
            Log.d(TAG, "TtsSentence received, ensuring state is SPEAKING")
            _subState.value = ConversationSubState.SPEAKING
            syncSubState()
        }
    }

    private fun handleTtsAudioFrame(data: ByteArray) {
        if (!_isMuted.value && _subState.value == ConversationSubState.SPEAKING)
            audioService.play(data)
    }

    private fun handleDialogueEnd() {
        Log.d(TAG, "DialogueEnd received, deactivating audio")
        cleanConversation()
    }

    fun startConversation(contextData: ByteArray? = null) {
        if (!netCommService.isConnected) return
        isActive = true
        isAutoMode = true
        resetRoundText()
        _subState.value = ConversationSubState.LISTENING
        syncSubState()

        netCommService.startListening("auto")
        contextData?.let { netCommService.sendAudio(it) }
        audioService.activate()
    }

    fun interrupt() {
        netCommService.abort("user_interrupt")
        cleanConversation()
    }

    fun stopAutoConversation() {
        netCommService.abort("stop_auto_mode")
        cleanConversation()
    }

    private fun resetRoundText() {
        _currentRoundUserText.value = null
        _currentRoundAiText.value = null
    }

    private fun cleanConversation(){
        Log.d(TAG, "cleanConversation: Deactivating audio and resetting state")
        isActive = false
        isAutoMode = false

        audioService.deactivate()
        audioService.stopPlaying()

        resetRoundText()

        robotStateEngine.updateEngineState(RobotEngineState.Ready)
    }

    private fun startNextRound() {
        if (!isAutoMode || !netCommService.isConnected) {
            isActive = false
            audioService.deactivate()
            return
        }
        isActive = true
        resetRoundText()
        _subState.value = ConversationSubState.LISTENING
        syncSubState()

        netCommService.startListening("auto")
        audioService.activate()
    }
}
