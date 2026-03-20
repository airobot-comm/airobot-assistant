package com.airobot.tablet.airobotui.viewmodel

import androidx.lifecycle.ViewModel
import com.airobot.tablet.airobotui.state.RobotEngineState
import com.airobot.tablet.airobotui.state.RobotStateEngine
import com.airobot.tablet.airobotui.state.ServiceSubState
import com.airobot.tablet.airobotui.state.ServiceCardData
import com.airobot.tablet.airobotui.state.TimerCardData
import com.airobot.tablet.airobotui.state.ServiceCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * 功能服务模块 ViewModel
 */
@HiltViewModel
class ServiceViewModel @Inject constructor(
    private val robotStateEngine: RobotStateEngine
) : ViewModel() {

    private val _activeCard = MutableStateFlow<ServiceCard?>(null)
    val activeCard: StateFlow<ServiceCard?> = _activeCard.asStateFlow()

    private val _serviceSubState = MutableStateFlow(ServiceSubState.IDLE)
    val serviceSubState: StateFlow<ServiceSubState> = _serviceSubState.asStateFlow()

    private val _activeServiceData = MutableStateFlow<ServiceCardData?>(null)
    val activeServiceData: StateFlow<ServiceCardData?> = _activeServiceData.asStateFlow()

    /**
     * 开启服务卡片
     */
    fun startService(card: ServiceCard) {
        _activeCard.value = card
        _serviceSubState.value = ServiceSubState.IDLE
        // 清除旧的业务数据
        _activeServiceData.value = null

        robotStateEngine.updateEngineState(RobotEngineState.FunctionService(card.id, _serviceSubState.value))
    }

    /**
     * 关闭服务
     */
    fun closeService() {
        _activeCard.value = null
        _serviceSubState.value = ServiceSubState.IDLE
        _activeServiceData.value = null
        robotStateEngine.updateEngineState(RobotEngineState.Ready)
    }

    /**
     * 处理计时器动作
     */
    fun handleTimerAction(action: String) {
        when (action) {
            "PAUSE" -> {
                _serviceSubState.value = ServiceSubState.PAUSED
            }
            "RESUME" -> {
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
            robotStateEngine.updateEngineState(RobotEngineState.FunctionService(card.id, _serviceSubState.value))
        }
    }

    /**
     * 模拟启动计时器
     */
    fun startTimer(task: String, duration: Int) {
        _activeServiceData.value = TimerCardData(duration, task)
        _serviceSubState.value = ServiceSubState.RUNNING
        syncToMainState()
    }
}


