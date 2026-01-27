package com.airobotcomm.tablet.airobotui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airobotcomm.tablet.comm.NetworkService
import com.airobotcomm.tablet.comm.NetworkState
import com.airobotcomm.tablet.comm.protocol.AiRobotEvent
import com.airobotcomm.tablet.airobotui.state.RobotState
import com.airobotcomm.tablet.airobotui.state.RobotStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.airobotcomm.tablet.domain.OtaManager
import com.airobotcomm.tablet.domain.OtaState

/**
 * 主控制 ViewModel
 * 负责机器人一级状态的管理与分发
 */
@HiltViewModel
class RobotMainViewModel @Inject constructor(
    private val networkService: NetworkService,
    private val robotStateManager: RobotStateManager,
    private val otaManager: OtaManager
) : ViewModel() {

    val robotState: StateFlow<RobotState> = robotStateManager.robotState

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showActivationDialog = MutableStateFlow(false)
    val showActivationDialog: StateFlow<Boolean> = _showActivationDialog.asStateFlow()

    private val _activationCode = MutableStateFlow<String?>(null)
    val activationCode: StateFlow<String?> = _activationCode.asStateFlow()

    init {
        observeNetwork()
        observeOta()
        startOtaCheck()
    }

    private fun startOtaCheck() {
        viewModelScope.launch {
            otaManager.checkUpdateAndActivation()
        }
    }

    private fun observeOta() {
        viewModelScope.launch {
            otaManager.state.collect { state ->
                when (state) {
                    is OtaState.Checking -> {
                        robotStateManager.updateRobotState(RobotState.Initializing)
                    }
                    is OtaState.ActivationRequired -> {
                        _activationCode.value = state.code
                        _showActivationDialog.value = true
                        robotStateManager.updateRobotState(RobotState.Unauthorized(state.code))
                    }
                    is OtaState.Activated -> {
                        _showActivationDialog.value = false
                        _activationCode.value = null
                        // OTA 激活/检查完成后，尝试连接网络
                        networkService.connect()
                    }
                    is OtaState.UpdateAvailable -> {
                        // TODO: 处理更新提示
                        robotStateManager.updateRobotState(RobotState.Ready)
                    }
                    is OtaState.Error -> {
                        _errorMessage.value = state.message
                        robotStateManager.updateRobotState(RobotState.Offline)
                    }
                    is OtaState.Idle -> {}
                }
            }
        }
    }

    private fun observeNetwork() {
        // 监听网络状态
        viewModelScope.launch {
            networkService.state.collect { state ->
                when (state) {
                    NetworkState.CONNECTING, NetworkState.RECONNECTING -> {
                        robotStateManager.updateRobotState(RobotState.Connecting)
                    }
                    NetworkState.CONNECTED -> {
                        // 由 handleAiRobotEvent(AiRobotEvent.Connected) 处理
                    }
                    NetworkState.ERROR, NetworkState.IDLE -> {
                        // 避免在已经 Unauthorized 的情况下切回 Offline，除非确实断开了
                        if (robotStateManager.robotState.value !is RobotState.Unauthorized && 
                            robotStateManager.robotState.value !is RobotState.Initializing) {
                            robotStateManager.updateRobotState(RobotState.Offline)
                        }
                    }
                }
            }
        }

        // 监听特定事件
        viewModelScope.launch {
            networkService.events.collect { event ->
                handleAiRobotEvent(event)
            }
        }
    }

    private fun handleAiRobotEvent(event: AiRobotEvent) {
        when (event) {
            is AiRobotEvent.Connected -> {
                robotStateManager.updateRobotState(RobotState.Ready)
                _errorMessage.value = null
            }
            is AiRobotEvent.Disconnected -> {
                robotStateManager.updateRobotState(RobotState.Offline)
            }
            is AiRobotEvent.Error -> {
                _errorMessage.value = event.message
            }
            else -> {}
        }
    }

    fun onActivationConfirmed() {
        val code = _activationCode.value
        if (code != null) {
            viewModelScope.launch {
                otaManager.confirmActivation(code)
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * 更新一级状态
     */
    fun updateRobotState(newState: RobotState) {
        robotStateManager.updateRobotState(newState)
    }
}
