package com.airobot.services.state

/**
 * 服务卡片子状态
 */
enum class ServiceSubState {
    IDLE,       // 空闲
    RUNNING,    // 运行中
    PAUSED,     // 已暂停
    COMPLETED,  // 已完成
    CANCELLED   // 已取消
}

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
 * 服务卡片具体数据接口
 */
sealed interface ServiceCardData

/**
 * 专注时钟数据
 */
data class TimerCardData(
    val duration: Int,  // 时长（秒）
    val task: String    // 任务名称
) : ServiceCardData

/**
 * 服务卡片数据
 */
data class ServiceCard(
    val id: String,
    val type: ServiceCardType,
    val title: String,
    val content: String,
    val statusTip: String,
    val iconResId: Int,
    val demoContent: String? = null
)
