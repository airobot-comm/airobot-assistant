package com.airobot.assistant.airobotui.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 集中管理机器人状态，避免 ViewModel 循环依赖
 */
@Singleton
class RobotStateManager @Inject constructor() {
    private val _robotState = MutableStateFlow<RobotState>(RobotState.Offline)
    val robotState: StateFlow<RobotState> = _robotState.asStateFlow()

    fun updateRobotState(newState: RobotState) {
        _robotState.value = newState
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
    PAUSED      // 已暂停
}

/**
 * 一级状态：AI 机器人整体状态
 */
sealed class RobotState {
    object Offline : RobotState()                                               // 离线
    object Initializing : RobotState()                                          // OTA/初始化/报备中
    data class Unauthorized(val code: String) : RobotState()                    // 未激活/认证失败，带激活码
    object Connecting : RobotState()                                            // WebSocket/协议握手连接中
    object Ready : RobotState()                                                 // 准备就绪/等待
    data class Conversation(val subState: ConversationSubState) : RobotState()    // 对话中
    data class FunctionService(
        val serviceId: String,
        val subState: ServiceSubState
    ) : RobotState()                                                            // 功能卡片服务中
}

