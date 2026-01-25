package com.airobotcomm.tablet.airobotui.viewmodel

import androidx.lifecycle.ViewModel
import com.airobotcomm.tablet.airobotui.state.RobotState
import com.airobotcomm.tablet.airobotui.state.RobotStateManager
import com.airobotcomm.tablet.airobotui.state.ServiceSubState
import com.airobotcomm.tablet.airobotui.state.TimerCommand
import com.airobotcomm.tablet.airobotui.state.TimerStatus
import com.airobotcomm.tablet.airobotui.state.ServiceCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * 功能服务模块 ViewModel
 */
@HiltViewModel
class ServiceViewModel @Inject constructor(
    private val robotStateManager: RobotStateManager
) : ViewModel() {

    private val _activeCard = MutableStateFlow<ServiceCard?>(null)
    val activeCard: StateFlow<ServiceCard?> = _activeCard.asStateFlow()

    private val _serviceSubState = MutableStateFlow(ServiceSubState.IDLE)
    val serviceSubState: StateFlow<ServiceSubState> = _serviceSubState.asStateFlow()

    private val _timerCommand = MutableStateFlow<TimerCommand?>(null)
    val timerCommand: StateFlow<TimerCommand?> = _timerCommand.asStateFlow()

    private val _timerStatus = MutableStateFlow(TimerStatus.IDLE)
    val timerStatus: StateFlow<TimerStatus> = _timerStatus.asStateFlow()

    /**
     * 开启服务卡片
     */
    fun startService(card: ServiceCard) {
        _activeCard.value = card
        _serviceSubState.value = ServiceSubState.IDLE
        robotStateManager.updateRobotState(RobotState.FunctionService(card.id, _serviceSubState.value))
    }

    /**
     * 关闭服务
     */
    fun closeService() {
        _activeCard.value = null
        _serviceSubState.value = ServiceSubState.IDLE
        _timerStatus.value = TimerStatus.IDLE
        _timerCommand.value = null
        robotStateManager.updateRobotState(RobotState.Ready)
    }

    /**
     * 处理计时器动作
     */
    fun handleTimerAction(action: String) {
        when (action) {
            "PAUSE" -> {
                _timerStatus.value = TimerStatus.PAUSED
                _serviceSubState.value = ServiceSubState.PAUSED
            }
            "RESUME" -> {
                _timerStatus.value = TimerStatus.RUNNING
                _serviceSubState.value = ServiceSubState.RUNNING
            }
            "STOP" -> {
                closeService()
                return
            }
        }
        syncToMainState()
    }

    private fun syncToMainState() {
        _activeCard.value?.let { card ->
            robotStateManager.updateRobotState(RobotState.FunctionService(card.id, _serviceSubState.value))
        }
    }

    /**
     * 模拟启动计时器
     */
    fun startTimer(task: String, duration: Int) {
        _timerCommand.value = TimerCommand(duration, task)
        _timerStatus.value = TimerStatus.RUNNING
        _serviceSubState.value = ServiceSubState.RUNNING
        syncToMainState()
    }
}
