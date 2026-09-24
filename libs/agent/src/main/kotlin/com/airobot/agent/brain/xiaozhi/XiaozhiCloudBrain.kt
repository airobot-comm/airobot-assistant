package com.airobot.agent.brain.xiaozhi

import android.content.Context
import android.util.Log
import com.airobot.agent.audio.AudioEvent
import com.airobot.agent.audio.AudioService
import com.airobot.agent.brain.AiBrain
import com.airobot.agent.brain.BrainState
import com.airobot.agent.brain.mcp.McpHandler
import com.airobot.agent.brain.model.Message
import com.airobot.agent.brain.model.MessageRole
import com.airobot.agent.brain.session.ConversationSession
import com.airobot.core.comm.NetCommEvent
import com.airobot.core.comm.NetCommService
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.airobot.agent.manager.AgentManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Xiaozhi cloud AI proxy — acts as the AiBrain implementation for the Xiaozhi cloud agent.
 * Handles WebSocket connection packets, AEC/VAD audio routing, and standardizes MCP request dispatching.
 */
@Singleton
class XiaozhiCloudBrain @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val netCommService: NetCommService,
    private val audioService: AudioService,
    private val gson: Gson,
    private val mcpHandler: McpHandler,
    private val session: ConversationSession,
    private val configManager: AgentManager
) : AiBrain {

    companion object {
        private const val TAG = "XiaozhiCloudBrain"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Delegate State Flows to ConversationSession
    override val brainState: StateFlow<BrainState> = session.brainState
    override val messages: StateFlow<List<Message>> = session.messages
    override val currentRoundUserText: StateFlow<String?> = session.currentRoundUserText
    override val currentRoundAiText: StateFlow<String?> = session.currentRoundAiText
    override val audioLevel: StateFlow<Float> = session.audioLevel

    // VAD Interruption Shielding State Machine to prevent echo leakage during playback transitions
    private enum class ShieldingMode {
        NONE,
        STOP_TRANSIENT,   // 200ms tail leakage shielding when TTS stops
        START_TRANSIENT   // 500ms start leakage shielding during AEC convergence
    }

    @Volatile
    private var currentShieldingMode = ShieldingMode.NONE

    @Volatile
    private var hasSpeechActiveDuringShielding = false

    @Volatile
    private var isFirstAudioFrameOfRound = true

    private var isBuffering = false
    private val audioBuffer = mutableListOf<ByteArray>()
    private var bufferJob: Job? = null

    init {
        // Collect Network Communication Events
        scope.launch {
            netCommService.events.collect { event ->
                try {
                    when (event) {
                        is NetCommEvent.TextMessage -> {
                            val json = gson.fromJson(event.json, JsonObject::class.java)
                            val type = json.get("type")?.asString ?: return@collect
                            val sessionId = json.get("session_id")?.asString

                            if (type == "mcp") {
                                val payload = json.getAsJsonObject("payload") ?: return@collect
                                scope.launch {
                                    val responsePayload = mcpHandler.handleMcpRequest(payload)
                                    val response = JsonObject().apply {
                                        sessionId?.let { addProperty("session_id", it) }
                                        addProperty("type", "mcp")
                                        add("payload", responsePayload)
                                    }
                                    netCommService.sendRawText(gson.toJson(response))
                                }
                            } else if (session.isActive) {
                                when (type) {
                                    "stt" -> {
                                        val text = json.get("text")?.asString ?: ""
                                        if (text.isNotBlank()) handleSttResult(text)
                                    }
                                    "tts" -> {
                                        val state = json.get("state")?.asString
                                        when (state) {
                                            "start" -> handleTtsStart()
                                            "stop" -> handleTtsStop()
                                            "sentence_start" -> {
                                                val text = json.get("text")?.asString ?: ""
                                                if (text.isNotBlank()) handleTtsSentence(text)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        is NetCommEvent.AudioFrame -> {
                            if (session.isActive) {
                                handleTtsAudioFrame(event.data)
                            }
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing network event", e)
                }
            }
        }

        // Collect Audio Events (VAD / Speech Level)
        scope.launch {
            audioService.events.collect { event ->
                if (session.isActive || event is AudioEvent.VoiceLevel) {
                    handleAudioEvent(event)
                }
            }
        }

        Log.d(TAG, "XiaozhiCloudBrain initialized, listening for events")
    }

    private fun handleAudioEvent(event: AudioEvent) {
        when (event) {
            is AudioEvent.SpeechData -> {
                if (session.brainState.value == BrainState.LISTENING) {
                    if (isBuffering) {
                        audioBuffer.add(event.data)
                    } else {
                        netCommService.sendAudio(event.data)
                    }
                }
            }
            is AudioEvent.VoiceLevel -> {
                session.updateAudioLevel(event.level)
            }
            is AudioEvent.SpeechStart -> {
                Log.d(TAG, "SpeechStart received, brainState=${session.brainState.value}, shieldingMode=$currentShieldingMode")
                
                // Process shielding modes first
                when (currentShieldingMode) {
                    ShieldingMode.STOP_TRANSIENT -> {
                        Log.d(TAG, "SpeechStart ignored: STOP_TRANSIENT shielding active (fading out echo)")
                        return
                    }
                    ShieldingMode.START_TRANSIENT -> {
                        Log.d(TAG, "SpeechStart ignored: START_TRANSIENT shielding active, flagging deferred speech")
                        hasSpeechActiveDuringShielding = true
                        return
                    }
                    ShieldingMode.NONE -> {
                        if (session.brainState.value == BrainState.SPEAKING) {
                            Log.d(TAG, "User started speaking while AI is speaking, interrupting...")
                            interruptSpeak(source = "vad_interrupt")
                        }
                    }
                }

                if (session.brainState.value == BrainState.LISTENING) {
                    Log.d(TAG, "Starting manual listening session on ASR server")
                    netCommService.startListening("manual")
                    isBuffering = true
                    audioBuffer.clear()
                    bufferJob?.cancel()
                    bufferJob = scope.launch {
                        delay(300)
                        isBuffering = false
                        val combined = audioBuffer.toList()
                        audioBuffer.clear()
                        combined.forEach {
                            netCommService.sendAudio(it)
                        }
                    }
                }
            }
            is AudioEvent.SpeechEnd -> {
                Log.d(TAG, "SpeechEnd received, brainState=${session.brainState.value}, shieldingMode=$currentShieldingMode")
                if (currentShieldingMode == ShieldingMode.START_TRANSIENT) {
                    Log.d(TAG, "SpeechEnd received during START_TRANSIENT shielding, clearing deferred speech flag")
                    hasSpeechActiveDuringShielding = false
                    return
                }
                if (session.brainState.value == BrainState.LISTENING) {
                    Log.d(TAG, "User finished speaking. Stopping listening session and transitioning to THINKING")
                    netCommService.stopListening()
                    session.setBrainState(BrainState.THINKING)
                    session.startThinkingTimeout {
                        Log.w(TAG, "Thinking timeout reached! Reverting to LISTENING.")
                        startNextRound()
                    }
                }
            }
            else -> {}
        }
    }

    private fun handleTtsStart() {
        Log.d(TAG, "TTS Start received, transitioning to SPEAKING")
        session.cancelThinkingTimeout()
        session.resetInactivityTimer {
            stopAutoConversation()
        }
        session.setBrainState(BrainState.SPEAKING)

        // Reset first audio frame flag for this round.
        // We defer shielding to start when the first audio packet is actually played.
        isFirstAudioFrameOfRound = true

        if (!configManager.config.value.isSpeechInterruptionEnabled) {
            Log.d(TAG, "Speech Interruption disabled (Half-Duplex): Deactivating audio during TTS playback")
            audioService.deactivate()
        }
    }

    private fun handleTtsStop() {
        audioService.stopPlaying()

        if (session.brainState.value != BrainState.SPEAKING) {
            Log.d(TAG, "Ignoring TtsStop because brainState is ${session.brainState.value} (likely interrupted)")
            return
        }

        // Activate tail echo shielding (STOP_TRANSIENT) for 200ms when transitioning to Listening
        currentShieldingMode = ShieldingMode.STOP_TRANSIENT

        scope.launch {
            delay(200)
            if (currentShieldingMode == ShieldingMode.STOP_TRANSIENT) {
                currentShieldingMode = ShieldingMode.NONE
            }
            if (session.brainState.value != BrainState.SPEAKING) {
                Log.d(TAG, "Ignoring TtsStop transition because brainState changed during delay")
                return@launch
            }

            if (session.isAutoMode) {
                startNextRound()
            } else {
                cleanConversation()
            }
        }
    }

    private fun handleTtsSentence(text: String) {
        session.handleTtsSentence(text)
        if (session.brainState.value != BrainState.SPEAKING) {
            Log.d(TAG, "TtsSentence received, ensuring state is SPEAKING")
            session.setBrainState(BrainState.SPEAKING)
        }
    }

    private fun handleTtsAudioFrame(data: ByteArray) {
        if (session.brainState.value == BrainState.SPEAKING) {
            if (isFirstAudioFrameOfRound) {
                isFirstAudioFrameOfRound = false
                Log.d(TAG, "First TTS audio frame received, activating 600ms START_TRANSIENT shielding window.")
                
                // Activate AEC convergence shielding (START_TRANSIENT) for 600ms to ignore initial echo leakage.
                // 600ms allows ~100ms for player device buffering startup + ~500ms for hardware AEC filter convergence.
                currentShieldingMode = ShieldingMode.START_TRANSIENT
                hasSpeechActiveDuringShielding = false
                scope.launch {
                    delay(600)
                    if (currentShieldingMode == ShieldingMode.START_TRANSIENT) {
                        currentShieldingMode = ShieldingMode.NONE
                        // If VAD speech is still active after the shielding window, trigger deferred interruption
                        if (hasSpeechActiveDuringShielding && session.brainState.value == BrainState.SPEAKING) {
                            Log.d(TAG, "Speech detected during start shielding window persisted, triggering deferred interruption")
                            interruptSpeak(source = "vad_interrupt")
                        }
                    }
                }
            }
            audioService.play(data)
        }
    }

    private fun handleSttResult(text: String) {
        session.cancelThinkingTimeout()
        if (text.isNotBlank()) {
            session.handleSttResult(text)
            if (session.brainState.value == BrainState.LISTENING) {
                Log.d(TAG, "STT received, transitioning LISTENING -> THINKING")
                session.setBrainState(BrainState.THINKING)
            }
        }
    }

    private fun cleanConversation() {
        Log.d(TAG, "cleanConversation: Deactivating audio and resetting state")
        bufferJob?.cancel()
        isBuffering = false
        audioBuffer.clear()
        
        currentShieldingMode = ShieldingMode.NONE
        hasSpeechActiveDuringShielding = false
        isFirstAudioFrameOfRound = true

        audioService.deactivate()
        audioService.stopPlaying()

        session.stopSession()
    }

    private fun startNextRound() {
        bufferJob?.cancel()
        isBuffering = false
        audioBuffer.clear()
        
        currentShieldingMode = ShieldingMode.NONE
        hasSpeechActiveDuringShielding = false
        isFirstAudioFrameOfRound = true

        if (!session.isAutoMode || !netCommService.isConnected) {
            audioService.deactivate()
            session.stopSession()
            return
        }
        session.startNextRound()

        audioService.activate()
    }

    // --- Conversation Control APIs ---

    override fun startConversation(contextData: ByteArray?) {
        if (!netCommService.isConnected) return
        session.startSession(autoMode = true)
        session.setBrainState(BrainState.LISTENING)

        audioService.activate()

        if (contextData != null) {
            Log.d(TAG, "startConversation with contextData: starting manual listening session")
            netCommService.startListening("manual")
            netCommService.sendAudio(contextData)
        }
    }

    override fun stopAutoConversation() {
        netCommService.abort("stop_auto_mode")
        cleanConversation()
    }

    override fun interrupt() {
        netCommService.abort("user_interrupt")
        cleanConversation()
    }

    override fun interruptSpeak(source: String) {
        if (session.brainState.value != BrainState.SPEAKING) return

        Log.d(TAG, "interruptSpeak triggered by source: $source, stopping playback and resuming listening")

        // Add system message indicating speech was interrupted
        session.addMessage(
            Message(
                role = MessageRole.SYSTEM,
                content = context.getString(com.airobot.agent.R.string.dialogue_system_interrupted)
            )
        )

        netCommService.abort("user_interrupt")
        audioService.stopPlaying()
        startNextRound()
    }

    override fun isSessionActive(): Boolean = session.isActive

    override fun injectSystemMessage(content: String) {
        session.addMessage(Message(role = MessageRole.SYSTEM, content = content))
    }

    override fun wakeUp() {
        session.setBrainState(BrainState.LISTENING)
        netCommService.startListening("auto")
        Log.d(TAG, "wakeUp: switched to LISTENING")
    }

    override fun sleep() {
        session.setBrainState(BrainState.IDLE)
        netCommService.stopListening()
        Log.d(TAG, "sleep: switched to IDLE")
    }
}
