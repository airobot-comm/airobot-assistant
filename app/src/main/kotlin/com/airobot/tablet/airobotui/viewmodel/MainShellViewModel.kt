package com.airobot.tablet.airobotui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.airobot.tablet.system.SysManage
import com.airobot.tablet.system.SysState
import com.airobot.tablet.system.model.AiAgent
import com.airobot.tablet.system.model.DeviceInfo
import com.airobot.tablet.system.model.SystemInfo
import com.airobot.core.comm.NetCommService
import com.airobot.core.comm.NetworkState
import com.airobot.core.comm.NetCommEvent
import com.airobot.character.state.RobotEngineState
import com.airobot.character.state.RobotStateEngine
import com.airobot.audio.AudioEvent
import com.airobot.audio.AudioService

/**
 * 娑撶粯甯堕崚?ViewModel
 * 鐠愮喕鐭楅張鍝勬珤娴滆桨绔寸痪褏濮搁幀浣烘畱缁狅紕鎮婃稉搴″瀻閸?
 */
@HiltViewModel
class MainShellViewModel @Inject constructor(
    private val netCommService: NetCommService,
    private val robotStateEngine: RobotStateEngine,
    private val sysManage: SysManage,
    private val audioService: AudioService
) : ViewModel() {

    val robotState: StateFlow<RobotEngineState> = robotStateEngine.robotEngineState
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
     * 閸掓繂顫愰崠鏍叾妫版垶婀囬崝?
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
                        val currentState = robotStateEngine.robotEngineState.value
                        Log.d("MainShellViewModel",
                            "Wakeup detected. Current state: $currentState")

                        // 閺嶇绺剧€瑰鍙忛柅鏄忕帆閿涙碍顥呴弻銉ㄧ箻閸忋儱顕拠婵堟畱閸忓牆鍠呴弶鈥叉閿涘苯褰ч張澶婂徔婢跺洦娼禒鑸靛閻喐顒滈崥顖氬З鐎电鐦?
                        val isNetworkReady = netCommService.isConnected
                        val isSystemReady = sysManage.state.value is com.airobot.tablet.system.SysState.Ready
                        if (isNetworkReady && isSystemReady &&
                            (currentState is RobotEngineState.Ready || currentState is RobotEngineState.Conversation)) {
                            Log.d("MainShellViewModel", "Conditions met. Proceeding with wakeup.")
                            viewModelScope.launch {
                                _wakeupEvent.emit(Unit)
                            }
                        } else {
                            // 鐎瑰鍙忛崶鐐衡偓鈧敍姘瑝濠娐ゅ喕閺夆€叉閿涘苯宸遍崚璺虹殺闂婃娊顣堕張宥呭閹峰娲?WAITING 閻樿埖鈧緤绱濋崑婊勵剾閺佺増宓侀崣鎴︹偓?
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
                        robotStateEngine.updateEngineState(RobotEngineState.Initializing)
                    }
                    is SysState.DeviceActivationRequired -> {
                        robotStateEngine.updateEngineState(
                            RobotEngineState.Unauthorized("DEVICE_ACTIVATION"))
                        _showActivationDialog.value = false
                    }
                    is SysState.AiRobotActivationRequired -> {
                        val code = state.code
                        _activationCode.value = code
                        _showActivationDialog.value = true
                        robotStateEngine.updateEngineState(
                            RobotEngineState.Unauthorized(code))
                    }
                    is SysState.Ready -> {
                        _showActivationDialog.value = false
                        _activationCode.value = null
                        // OTA 濠碘偓濞?濡偓閺屻儱鐣幋鎰倵閿涘苯鐨剧拠鏇＄箾閹恒儳缍夌紒?
                        netCommService.connect()
                        // 鐏忔繆鐦崚婵嗩潗閸栨牠鐓舵０鎴炴箛閸?(闁板秶鐤嗛崣顖欑矤 SystemInfo 閼惧嘲褰囬敍灞绢劃婢跺嫭娈忛悽銊╃帛鐠?
                        // 濞夈劍鍓伴敍姘剁叾妫版垵鍨垫慨瀣閸欘垵鍏橀棁鈧憰浣规綀闂勬劧绱濋柅姘埗閸?UI 鐏炲倹鍨ㄧ涵顔荤箽閺夊啴妾洪崥搴ょ殶閻劊鈧?
                        // 鏉╂瑩鍣烽弳鍌涙娑撳秷鐨熼悽?audioService.init閿涘瞼鏁?ConversationViewModel 閹?MainViewModel 閻?UI Event 鐟欙箑褰?
                    }
                    is SysState.UpdateAvailable -> {
                        // TODO: 婢跺嫮鎮婇弴瀛樻煀閹绘劗銇?
                        robotStateEngine.updateEngineState(RobotEngineState.Ready)
                        // Also try to connect if update is available, assuming it functions
                        netCommService.connect()
                    }
                    is SysState.Error -> {
                        _errorMessage.value = state.message
                        robotStateEngine.updateEngineState(RobotEngineState.Offline)
                    }
                    is SysState.Idle -> {}
                }
            }
        }
    }

    private fun observeNetwork() {
        // 閻╂垵鎯夌純鎴犵捕閻樿埖鈧?
        viewModelScope.launch {
            netCommService.state.collect { state ->
                when (state) {
                    NetworkState.CONNECTING, NetworkState.RECONNECTING -> {
                        robotStateEngine.updateEngineState(RobotEngineState.Connecting)
                    }
                    NetworkState.CONNECTED -> {
                        // 閻?handleAiRobotEvent(NetCommEvent.Connected) 婢跺嫮鎮?
                    }
                    NetworkState.ERROR, NetworkState.IDLE -> {
                        Log.w("MainShellViewModel", "Network state is $state. Deactivating Audio Service.")
                        audioService.deactivate() // 瀵搫鍩楅崶鐐衡偓鈧棅鎶筋暥閺堝秴濮?

                        // 闁灝鍘ら崷銊ュ嚒缂?Unauthorized 閻ㄥ嫭鍎忛崘鍏哥瑓閸掑洤娲?Offline閿涘矂娅庨棃鐐碘€樼€圭偞鏌囧鈧禍?
                        if (robotStateEngine.robotEngineState.value !is RobotEngineState.Unauthorized && 
                            robotStateEngine.robotEngineState.value !is RobotEngineState.Initializing) {
                            robotStateEngine.updateEngineState(RobotEngineState.Offline)
                        }
                    }
                }
            }
        }

        // 閻╂垵鎯夐悧鐟扮暰娴滃娆?
        viewModelScope.launch {
            netCommService.events.collect { event ->
                handleAiRobotEvent(event)
            }
        }
    }

    private fun handleAiRobotEvent(event: NetCommEvent) {
        when (event) {
            is NetCommEvent.Connected -> {
                // 娴犲懎婀棃鐐差嚠鐠囨繄濮搁幀浣风瑓閸掑洤娲?Ready閿涘矂妲诲銏ｎ洬閻╂牕顕拠婵堝Ц閹?
                if (robotStateEngine.robotEngineState.value !is RobotEngineState.Conversation) {
                    Log.d("MainShellViewModel", "Safe transitioning to Ready due to Connected event")
                    robotStateEngine.updateEngineState(RobotEngineState.Ready)
                }
                _errorMessage.value = null
            }
            is NetCommEvent.Disconnected -> {
                Log.w("MainShellViewModel", "Received Disconnected event. Deactivating Audio Service.")
                audioService.deactivate()
                robotStateEngine.updateEngineState(RobotEngineState.Offline)
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


