package com.airobot.tablet.airobotui.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 集中管理机器人状态，避免 ViewModel 循环依赖。
 * 这个类是系统后端逻辑的唯一状态机 (State Engine)。
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
 * 二级状态：对话子状态
 */
enum class ConversationSubState {
    LISTENING,  // 聆听中
    THINKING,   // 思考中
    SPEAKING    // 说话中
}

/**
 * 二级状态：功能服务子状态
 */
enum class ServiceSubState {
    IDLE,       // 空闲
    RUNNING,    // 运行中
    PAUSED,     // 已暂停
    COMPLETED,  // 已完成
    CANCELLED   // 已取消
}

/**
 * 一级状态：AI 机器人整体状态 (系统底层引擎真相)
 */
sealed class RobotEngineState {
    object Offline : RobotEngineState()                                               // 离线
    object Initializing : RobotEngineState()                                          // OTA/初始化/报备中
    data class Unauthorized(val code: String) : RobotEngineState()                    // 未激活/认证失败，带激活码
    object Connecting : RobotEngineState()                                            // WebSocket/协议握手连接中
    object Ready : RobotEngineState()                                                 // 准备就绪/等待
    data class Conversation(val subState: ConversationSubState) : RobotEngineState()  // 对话中
    data class FunctionService(
        val serviceId: String,
        val subState: ServiceSubState
    ) : RobotEngineState()                                                            // 功能卡片服务中
}


