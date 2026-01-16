package com.airobotcomm.tablet.ui.components.robot

import com.airobotcomm.tablet.ui.viewmodel.ConversationState

/**
 * 机器人视觉状态 - 用于控制眼睛、天线等动画
 * 映射自 ConversationState，但专注于视觉表现
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
 * 将 ConversationState 映射到 RobotVisualState
 */
fun ConversationState.toRobotVisualState(): RobotVisualState {
    return when (this) {
        ConversationState.IDLE -> RobotVisualState.IDLE
        ConversationState.CONNECTING -> RobotVisualState.THINKING
        ConversationState.LISTENING -> RobotVisualState.LISTENING
        ConversationState.PROCESSING -> RobotVisualState.THINKING
        ConversationState.SPEAKING -> RobotVisualState.SPEAKING
    }
}

/**
 * 交互类型
 */
enum class InteractionType {
    CHAT,   // 普通聊天模式
    CARD    // 功能卡片模式
}

/**
 * 计时器状态
 */
enum class TimerStatus {
    IDLE,       // 空闲
    RUNNING,    // 运行中
    PAUSED      // 已暂停
}

/**
 * 计时器指令
 */
data class TimerCommand(
    val duration: Int,  // 时长（秒）
    val task: String    // 任务名称
)

/**
 * 服务卡片类型
 */
enum class ServiceCardType {
    TIMER,      // 专注时钟
    STORY,      // 故事时间
    CHAT,       // 随心聊天
    GAME,       // 益智游戏
    DRAW,       // 涂鸦创作
    QUIZ,       // 趣味问答
    ALARM,      // 闹钟
    WEATHER,    // 天气
    MUSIC       // 音乐
}

/**
 * 服务卡片数据
 */
data class ServiceCard(
    val id: String,
    val type: ServiceCardType,
    val title: String,
    val content: String,
    val statusTip: String,
    val iconResId: Int
)

/**
 * 机器人UI整体状态
 */
data class RobotUiState(
    val visualState: RobotVisualState = RobotVisualState.IDLE,
    val interactionType: InteractionType = InteractionType.CHAT,
    val timerStatus: TimerStatus = TimerStatus.IDLE,
    val timerCommand: TimerCommand? = null,
    val currentUserMsg: String? = null,
    val currentAiMsg: String? = null,
    val activeCard: ServiceCard? = null,
    val statusTip: String = "有什么可以帮你的？",
    val isConnected: Boolean = false
) {
    /**
     * 是否处于交互状态
     */
    val isInteracting: Boolean
        get() = (visualState != RobotVisualState.IDLE && visualState != RobotVisualState.SLEEPING) 
                || timerStatus != TimerStatus.IDLE
    
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
            timerStatus == TimerStatus.RUNNING -> "正在专注: ${timerCommand?.task ?: "未知任务"}..."
            timerStatus == TimerStatus.PAUSED -> "已暂停，休息一下..."
            else -> statusTip
        }
}
