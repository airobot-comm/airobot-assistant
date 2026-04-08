package com.airobot.character.state

import com.airobot.services.state.ServiceSubState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 闆嗕腑绠＄悊鏈哄櫒浜虹姸鎬侊紝閬垮厤 ViewModel 寰幆渚濊禆銆?
 * 杩欎釜绫绘槸绯荤粺鍚庣閫昏緫鐨勫敮涓€鐘舵€佹満 (State Engine)銆?
 */
@Singleton
class RobotStateEngine @Inject constructor() {
    private val _robotEngineState = MutableStateFlow<RobotEngineState>(RobotEngineState.Offline)
    val robotEngineState: StateFlow<RobotEngineState> = _robotEngineState.asStateFlow()

    fun updateEngineState(newState: RobotEngineState) {
        _robotEngineState.value = newState
    }
}


/**
 * 浜岀骇鐘舵€侊細瀵硅瘽瀛愮姸鎬?
 */
enum class ConversationSubState {
    LISTENING,  // 鑱嗗惉涓?
    THINKING,   // 鎬濊€冧腑
    SPEAKING    // 璇磋瘽涓?
}

/**
 * 涓€绾х姸鎬侊細AI 鏈哄櫒浜烘暣浣撶姸鎬?(绯荤粺搴曞眰寮曟搸鐪熺浉)
 */
sealed class RobotEngineState {
    object Offline : RobotEngineState()                                               // 绂荤嚎
    object Initializing : RobotEngineState()                                          // OTA/鍒濆鍖?鎶ュ涓?
    data class Unauthorized(val code: String) : RobotEngineState()                    // 鏈縺娲?璁よ瘉澶辫触锛屽甫婵€娲荤爜
    object Connecting : RobotEngineState()                                            // WebSocket/鍗忚鎻℃墜杩炴帴涓?
    object Ready : RobotEngineState()                                                 // 鍑嗗灏辩华/绛夊緟
    data class Conversation(val subState: ConversationSubState) : RobotEngineState()  // 瀵硅瘽涓?
    data class FunctionService(
        val serviceId: String,
        val subState: ServiceSubState
    ) : RobotEngineState()                                                            // 鍔熻兘鍗＄墖鏈嶅姟涓?
}


