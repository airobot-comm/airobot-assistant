package com.airobot.services

import androidx.lifecycle.ViewModel
import com.airobot.services.state.ServiceCard
import com.airobot.services.state.ServiceCardData
import com.airobot.services.state.ServiceSubState
import com.airobot.services.state.TimerCardData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * 跨应用平台通用功能服务模块 ViewModel
 */
@HiltViewModel
class ServiceViewModel @Inject constructor() : ViewModel() {

    private val _activeCard = MutableStateFlow<ServiceCard?>(null)
    val activeCard: StateFlow<ServiceCard?> = _activeCard.asStateFlow()

    private val _serviceSubState = MutableStateFlow(ServiceSubState.IDLE)
    val serviceSubState: StateFlow<ServiceSubState> = _serviceSubState.asStateFlow()

    private val _activeServiceData = MutableStateFlow<ServiceCardData?>(null)
    val activeServiceData: StateFlow<ServiceCardData?> = _activeServiceData.asStateFlow()

    /**
     * 打开服务卡片
     */
    fun startService(card: ServiceCard) {
        _activeCard.value = card
        _serviceSubState.value = ServiceSubState.IDLE
        _activeServiceData.value = null
    }

    /**
     * 关闭服务
     */
    fun closeService() {
        _activeCard.value = null
        _serviceSubState.value = ServiceSubState.IDLE
        _activeServiceData.value = null
    }

    /**
     * 处理计时器动作
     */
    fun handleTimerAction(action: String) {
        when (action) {
            "PAUSE" -> _serviceSubState.value = ServiceSubState.PAUSED
            "RESUME" -> _serviceSubState.value = ServiceSubState.RUNNING
            "STOP" -> {
                closeService()
            }
        }
    }

    /**
     * 模拟启动计时器
     */
    fun startTimer(task: String, duration: Int) {
        _activeServiceData.value = TimerCardData(duration, task)
        _serviceSubState.value = ServiceSubState.RUNNING
    }
}