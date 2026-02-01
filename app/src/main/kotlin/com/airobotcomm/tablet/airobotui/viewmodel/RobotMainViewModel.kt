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
import com.airobotcomm.tablet.system.SysManage
import com.airobotcomm.tablet.system.SysState
import com.airobotcomm.tablet.system.model.SystemInfo

/**
 * 主控制 ViewModel
 * 负责机器人一级状态的管理与分发
 */
@HiltViewModel
class RobotMainViewModel @Inject constructor(
    private val networkService: NetworkService,
    private val robotStateManager: RobotStateManager,
    private val sysManage: SysManage
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
        observeSysState()
        startSysCheck()
    }

    private fun startSysCheck() {
        // Trigger system initialization/check
        sysManage.start()
    }

    private fun observeSysState() {
        viewModelScope.launch {
            sysManage.state.collect { state ->
                when (state) {
                    is SysState.Checking -> {
                        robotStateManager.updateRobotState(RobotState.Initializing)
                    }
                    is SysState.ActivationRequired -> {
                        _activationCode.value = state.code
                        _showActivationDialog.value = true
                        robotStateManager.updateRobotState(RobotState.Unauthorized(state.code))
                    }
                    is SysState.Ready -> {
                        _showActivationDialog.value = false
                        _activationCode.value = null
                        // OTA 激活/检查完成后，尝试连接网络
                        networkService.connect()
                    }
                    is SysState.UpdateAvailable -> {
                        // TODO: 处理更新提示
                        robotStateManager.updateRobotState(RobotState.Ready)
                        // Also try to connect if update is available, assuming it functions
                        networkService.connect()
                    }
                    is SysState.Error -> {
                        _errorMessage.value = state.message
                        robotStateManager.updateRobotState(RobotState.Offline)
                    }
                    is SysState.Idle -> {}
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
                sysManage.activate(code)
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

    // System Config & Device Info Management
    val systemConfig: StateFlow<SystemInfo> = flow {
        emit(sysManage.getSystemInfo())
        // Poll or observe changes if SysManage supports it. For now, assume mainly updated via this VM.
    }.stateIn(viewModelScope, SharingStarted.Lazily, SystemInfo())

    // Device Info Exposure
    val deviceId = flow { emit(sysManage.getDevInfo().deviceId) }.stateIn(viewModelScope, SharingStarted.Lazily, "")
    val macAddress = flow { emit(sysManage.getDevInfo().macAddress) }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    fun updateConfig(newInfo: SystemInfo) {
        viewModelScope.launch {
            sysManage.updateSystemInfo(newInfo)
            // If SysManage doesn't emit updates, we might need a MutableStateFlow locally to reflect immediate changes,
            // but for now relying on SysManage being the source of truth or reloading.
            // Ideally SysManage.state or similar would emit config changes.
            // Since we don't see a config flow in SysState, we might need to rely on reloading.
        }
        // Retrigger connection logic as per original screen logic
        viewModelScope.launch {
            networkService.disconnect()
            robotStateManager.updateRobotState(RobotState.Offline)
            networkService.connect()
        }
    }
}
