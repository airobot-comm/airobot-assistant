package com.airobot.tablet.airobotui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.airobot.audio.AudioEvent
import com.airobot.audio.AudioService
import com.airobot.tablet.system.model.Message
import com.airobot.tablet.system.model.MessageRole
import com.airobot.core.comm.NetCommService
import com.airobot.core.comm.NetCommEvent
import com.airobot.character.airobotui.state.ConversationSubState
import com.airobot.character.airobotui.state.RobotEngineState
import com.airobot.character.airobotui.state.RobotStateEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay

/**
 * 鐎电鐦絍iewModel閿涘苯顦╅悶鍝籵nversation 閻樿埖鈧緤绱濇禍瀣╂閿涘苯褰傜挧宄邦嚠鐠囨繀绗岀紒鍫燁剾鐎电鐦?
 */
@HiltViewModel
class ConversationViewModel @Inject constructor(
    application: Application,
    private val netCommService: NetCommService,
    private val audioService: AudioService, // Use Interface
    private val robotStateEngine: RobotStateEngine
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "ConversationViewModel"
    }

    // 閸愬懘鍎存稉姘濡€崇础閿涘瞼濮搁幀浣圭垼韫囨绱濈€涙劗濮搁幀浣侯吀閻?
    private var isAutoMode = false    // 婢舵俺鐤嗙€电鐦介弨顖涘瘮
    private var isActive = false      // 娴兼俺鐦藉┑鈧ú缁樼垼韫囨绱濋悽銊ょ艾鏉╁洦鎶ゅ鑼矒濮濐澀绱扮拠婵堟畱瀵ゆ儼绻滃☉鍫熶紖
    private val _subState = MutableStateFlow(ConversationSubState.LISTENING)
    private val _isMuted = MutableStateFlow(false)  // 闂堟瑩鐓堕悩鑸碘偓浣侯吀閻?

    // conversation message handle:todo support
    private val _messages = MutableStateFlow<List<Message>>(emptyList())

    // 瑜版挸澧犳潪顔筋偧閻ㄥ嫮鏁ら幋鐤翻閸忋儲鏋冮張?
    private val _currentRoundUserText = MutableStateFlow<String?>(null)
    val currentRoundUserText: StateFlow<String?> = _currentRoundUserText.asStateFlow()
    
    // 瑜版挸澧犳潪顔筋偧閻ㄥ嚈I閸ョ偛顦查弬鍥ㄦ拱
    private val _currentRoundAiText = MutableStateFlow<String?>(null)
    val currentRoundAiText: StateFlow<String?> = _currentRoundAiText.asStateFlow()

    // 闂婃娊顣跺鍝勫閻樿埖鈧?
    private val _audioLevel = MutableStateFlow(0.0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    init {
        // startEventListening for airobot-comm
        viewModelScope.launch {
            netCommService.events.collect { event ->
                // 濞夈劍鍓伴敍姘涧婢跺嫮鎮婄€电鐦介幆鍛枌閻ㄥ嫮缍夌紒婊堚偓姘繆娴滃娆㈤敍灞藉従娴犳牜娈戞禍顦揳inviewModel婢跺嫮鎮?
                if (isActive){
                    handleAiRobotCommEvent(event)
                }
            }
        }

        // startEventListening for audio service
        viewModelScope.launch {
            // 濞夈劍鍓伴敍姘涧婢跺嫮鎮婄€电鐦介悩鑸碘偓浣风瑓Linsten鐎涙劗濮搁幀浣烘畱闂婃娊顣舵禍瀣╂閿涙岸鐓舵０鎴炴殶閹诡噯绱濋棅鎶藉櫤閿?
            // 閼板苯鏁滈柋鎺旂搼閻?MainViewModel婢跺嫮鎮婇敍娑㈡姜listen娑撳秴顦╅悶鍡樻暪闂婄绱漧evel鐠侊紕鐣婚懓灞筋嚤閼风ⅹI闁插秶绮敍鍫濇姎閻滃洣绗夋径鐕傜礆
            audioService.events.collect { event ->
                if (isActive && _subState.value == ConversationSubState.LISTENING){
                    handleAudioEvent(event)
                }
            }
        }
    }

    private fun handleAiRobotCommEvent(event: NetCommEvent) {
        when (event) {
            is NetCommEvent.STT -> {
                handleSttResult(event.text)
            }
            is NetCommEvent.TtsStart -> {
                handleTtsStart()
            }
            is NetCommEvent.TtsStop -> {
                handleTtsStop()
            }
            is NetCommEvent.TtsSentence -> {
                handleTtsSentence(event.text)
            }
            is NetCommEvent.AudioFrame -> {
                handleTtsAudioFrame(event.data)
            }
            is NetCommEvent.DialogueEnd -> {
                handleDialogueEnd()
            }
            else -> {
                // 閸忔湹绮柅姘繆瀵倸鐖堕敍宀€鏁眒ainviewModel婢跺嫮鎮?
            }
        }
    }

    private fun handleAudioEvent(event: AudioEvent) {
        when (event) {
            is AudioEvent.SpeechData -> {
                netCommService.sendAudio(event.data)
            }
            is AudioEvent.VoiceLevel -> {
                _audioLevel.value = event.level
            }
            else -> {
                return
            }
        }
    }

    private fun syncSubState() {
        val current = robotStateEngine.robotEngineState.value
        Log.d(TAG, "syncSubState: current=$current, target subState=${_subState.value}")
        // 閸忎浇顔忛崷?Ready, Conversation 閹?Connecting 閻樿埖鈧椒绗呮潻娑滎攽閸氬本顒?
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
            // 閺堝秴濮熼崳銊ヮ槱閻炲棗寮芥＃鍫窗閺堝妞傞崐?ttsStart 娴兼碍妫禍?STT 閸掓媽鎻?
            // 閸欘亣顩﹂弨璺哄煂 STT閿涘本鍨滄禒顒€姘ㄧ涵顔荤箽鐎瑰啳顫︾拋鏉跨秿楠炶埖妯夌粈?
            _currentRoundUserText.value = text
            addMessage(Message(role = MessageRole.USER, content = text))

            // 娴犲懎婀?LISTENING 閻樿埖鈧椒绗呴幍宥堢讣缁夎鍩?THINKING
            // 婵″倹鐏夊鑼病閺?SPEAKING閿涘矁顕╅弰?TTS 瀹歌尙绮″鈧慨瀣剁礉娣囨繃瀵?SPEAKING 閻樿埖鈧胶鎴风紒顓熸尡閹?
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
        // 閸楀厖绌舵潻妯荤梾閺€璺哄煂 STT (閺堝秴濮熼崳銊ヨ嫙閸欐垵顦╅悶鍡楁鏉?閿涘奔绡冮崗鍫濆瀼閸?SPEAKING閿涘苯娲滄稉鐑樻尡閹躲儱鍑＄紒蹇撶磻婵绨?
        _subState.value = ConversationSubState.SPEAKING
        syncSubState()
    }

    private fun handleTtsStop() {
        audioService.stopPlaying()
        viewModelScope.launch {
            // 瀵よ埖妞傛稉鈧悙瑙勬闂傜顔€鐎电鐦藉鍡楃潔缁€鐑樻纯闂€鎸庢闂?
            delay(200)
            if (isAutoMode) {
                // 閼奉亜濮╁Ο鈥崇础閿涙氨鎴风紒顓濈瑓娑撯偓鏉?
                startNextRound()
            } else {
                // 闂堢偠鍤滈崝銊δ佸蹇ョ窗瑜拌绨冲〒鍛倞鐎电鐦介崘鍛啇楠炶泛娲栭柅鈧悩鑸碘偓?
                cleanConversation()
            }
        }
    }

    private fun handleTtsSentence(text: String) {
        _currentRoundAiText.value = text
        addMessage(Message(role = MessageRole.AGENT, content = text))

        // 閸忔粌绨抽敍姘洤閺嬫粍鏁归崚鏉垮綖鐎涙劒绲炬潻妯荤梾閸掑洤鍩?SPEAKING閿涘矁藟閸掑洣绔存稉?
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
        // 鐎电鐦界紒鎾存将閿涘苯娲栭柅鈧獮鑸电闂勩倖婀板▎鈥愁嚠鐠囨繀淇婇幁?
        Log.d(TAG, "DialogueEnd received, deactivating audio")
        cleanConversation()
    }

    /**
     * 瀵偓閸氼垰顕拠婵撶礄AI 鐟欙箑褰傞幋鏍ㄥ閸斻劏袝閸欐埊绱?
     * @param contextData 閸炪倝鍟嬮弮鍓佹畱娑撳﹣绗呴弬鍥叾妫版垶鏆熼幑顕嗙礄閸欘垶鈧绱?
     */
    fun startConversation(contextData: ByteArray? = null) {
        if (!netCommService.isConnected) return
        isActive = true
        isAutoMode = true
        resetRoundText()
        _subState.value = ConversationSubState.LISTENING
        syncSubState()
        
        // 1. 濠碘偓濞茶崵缍夌紒婊€鐫涢崥?
        netCommService.startListening("auto")
        
        // 2. 婵″倹鐏夐張澶夌瑐娑撳鏋冮棅鎶筋暥閿涘苯褰傞柅浣风
        contextData?.let { netCommService.sendAudio(it) }
        
        // 3. 濠碘偓濞插鐓舵０鎴犫€栨禒?
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
        
        // 閺嶇绺鹃敍姘繁閸掑爼鐓舵０鎴炴箛閸斺€虫礀闁偓閸掓壆鐡戝鍛Ц閹緤绱濋崑婊勵剾娴犺缍嶉弫鐗堝祦閸欐垿鈧?
        audioService.deactivate()
        audioService.stopPlaying()

        // clean conversation text
        resetRoundText()

        // 閺勬儳绱￠柌宥囩枂閸忋劌鐪悩鑸碘偓浣稿煂 Ready
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

        // 瀵偓閸氼垱鏌婃潪顔筋偧缂冩垹绮舵笟锕€鎯?
        netCommService.startListening("auto")
        audioService.activate()
    }
}


