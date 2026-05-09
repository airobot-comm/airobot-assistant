package com.airobot.airbot.state

import com.airobot.services.state.ServiceCard
import com.airobot.services.state.ServiceCardData
import com.airobot.services.state.TimerCardData
import com.airobot.services.state.ServiceSubState


/**
 * 机器人视觉状态 - 用于控制眼睛、天线等动画
 * 映射自 RobotState，专注于视觉表现
 */
enum class RobotVisualState {
    IDLE,       // 空闲 - 渐变圆形眼睛 + 呼吸动画
    LISTENING,  // 聆听 - 高度脉冲动画
    THINKING,   // 思考 - 旋转加载环
    SPEAKING,   // 说话 - 缩放脉冲 + 嘴巴动画
    FOCUS,      // 专注 - 扁平禅意眼睛
    HAPPY,      // 开心 - 弯弯笑眼
    SLEEPING    // 睡眠 - 闭眼 + 缓慢呼吸
}

/**
 * 交互类型
 */
enum class InteractionType {
    CHAT,   // 普通聊天模式
    CARD    // 功能卡片模式
}


/**
 * 机器人 UI 整体展现状态 (唯一的 UI Truth Source)
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
     * 是否处于交互状态
     */
    val isInteracting: Boolean
        get() = (visualState != RobotVisualState.IDLE && visualState != RobotVisualState.SLEEPING) 
                || serviceSubState != ServiceSubState.IDLE 
                || activeCard != null
    
    /**
     * 是否为卡片模式
     */
    val isCardMode: Boolean
        get() = isInteracting && interactionType == InteractionType.CARD
    
    /**
     * 动态状态提示
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
