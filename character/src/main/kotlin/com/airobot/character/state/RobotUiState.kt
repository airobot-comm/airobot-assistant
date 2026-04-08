package com.airobot.character.state

import com.airobot.services.state.ServiceCard
import com.airobot.services.state.ServiceCardData
import com.airobot.services.state.TimerCardData
import com.airobot.services.state.ServiceSubState


/**
 * 鏈哄櫒浜鸿瑙夌姸鎬?- 鐢ㄤ簬鎺у埗鐪肩潧銆佸ぉ绾跨瓑鍔ㄧ敾
 * 鏄犲皠鑷?RobotState锛屼笓娉ㄤ簬瑙嗚琛ㄧ幇
 */
enum class RobotVisualState {
    IDLE,       // 绌洪棽 - 娓愬彉鍦嗗舰鐪肩潧 + 鍛煎惛鍔ㄧ敾
    LISTENING,  // 鑱嗗惉 - 楂樺害鑴夊啿鍔ㄧ敾
    THINKING,   // 鎬濊€?- 鏃嬭浆鍔犺浇鐜?
    SPEAKING,   // 璇磋瘽 - 缂╂斁鑴夊啿 + 鍢村反鍔ㄧ敾
    FOCUS,      // 涓撴敞 - 鎵佸钩绂呮剰鐪肩潧
    HAPPY,      // 寮€蹇?- 寮集绗戠溂
    SLEEPING    // 鐫＄湢 - 闂溂 + 缂撴參鍛煎惛
}

/**
 * 浜や簰绫诲瀷
 */
enum class InteractionType {
    CHAT,   // 鏅€氳亰澶╂ā寮?
    CARD    // 鍔熻兘鍗＄墖妯″紡
}




/**
 * 鏈哄櫒浜?UI 鏁翠綋灞曠幇鐘舵€?(鍞竴鐨?UI Truth Source)
 */
data class RobotUiState(
    // === UI Visual & System ===
    val visualState: RobotVisualState = RobotVisualState.IDLE,
    val isConnected: Boolean = false,
    
    // === Interaction & Dialogue ===
    val interactionType: InteractionType = InteractionType.CHAT,
    val currentUserMsg: String? = null,
    val currentAiMsg: String? = null,
    val statusTip: String = "有什么可以帮你的？",

    // === Active Service Data ===
    val activeCard: ServiceCard? = null,
    val serviceSubState: ServiceSubState = ServiceSubState.IDLE,
    val activeServiceData: ServiceCardData? = null
) {
    /**
     * 鏄惁澶勪簬浜や簰鐘舵€?
     */
    val isInteracting: Boolean
        get() = (visualState != RobotVisualState.IDLE && visualState != RobotVisualState.SLEEPING) 
                || serviceSubState != ServiceSubState.IDLE 
                || activeCard != null
    
    /**
     * 鏄惁涓哄崱鐗囨ā寮?
     */
    val isCardMode: Boolean
        get() = isInteracting && interactionType == InteractionType.CARD
    
    /**
     * 鍔ㄦ€佺姸鎬佹彁绀?
     */
    val dynamicStatusTip: String
        get() = when {
            serviceSubState == ServiceSubState.RUNNING -> {
                if (activeServiceData is TimerCardData) {
                    "正在专注: ${activeServiceData.task}..."
                } else {
                    "服务运行中..."
                }
            }
            serviceSubState == ServiceSubState.PAUSED -> "已暂停，休息一下..."
            else -> statusTip
        }
}


