package com.airobotcomm.tablet.domain.ota

import com.airobotcomm.tablet.domain.ota.model.DeviceReportRequest
import com.airobotcomm.tablet.domain.ota.model.OtaResponse
import com.airobotcomm.tablet.domain.ota.repository.OtaNetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OTA 状态
 */
sealed class OtaState {
    object Idle : OtaState()
    object Checking : OtaState()
    data class UpdateAvailable(val version: String, val url: String) : OtaState()
    data class ActivationRequired(val code: String) : OtaState()
    object Activated : OtaState()
    data class Error(val message: String) : OtaState()
}

/**
 * OTA 业务逻辑类 - 处理 OTA 相关的业务规则
 * 现在作为一个独立的领域服务运行
 */
@Singleton
class OtaManager @Inject constructor(
    private val otaNetRepository: OtaNetRepository,
    private val configManager: ConfigManager
) {
    private val _state = MutableStateFlow<OtaState>(OtaState.Idle)
    val state: StateFlow<OtaState> = _state.asStateFlow()

    /**
     * 检查更新和激活状态
     */
    suspend fun checkUpdateAndActivation() {
        _state.value = OtaState.Checking
        val config = configManager.loadConfig()
        
        if (config.otaUrl.isBlank()) {
            _state.value = OtaState.Error("OTA URL is not configured")
            return
        }

        val result = otaNetRepository.reportDeviceAndGetOta(
            clientId = config.uuid,
            deviceId = config.macAddress,
            otaUrl = config.otaUrl
        )

        result.onSuccess { response ->
            // 1. 检查激活状态
            val activationCode = response.activation?.code
            if (!activationCode.isNullOrEmpty() && config.activationCode.isEmpty()) {
                // 如果服务器返回了激活码，且本地没有激活码，则需要激活
                _state.value = OtaState.ActivationRequired(activationCode)
                return@onSuccess
            }

            // 2. 检查固件更新 (简化逻辑，实际可能需要版本号对比)
            // 这里假设服务器返回的 firmware.version 如果与本地不一致则提示更新
            // 目前先简单处理为已激活
            _state.value = OtaState.Activated
        }.onFailure { e ->
            _state.value = OtaState.Error(e.message ?: "OTA check failed")
        }
    }

    /**
     * 确认激活
     */
    suspend fun confirmActivation(code: String) {
        val currentConfig = configManager.loadConfig()
        configManager.saveConfig(currentConfig.copy(activationCode = code))
        _state.value = OtaState.Activated
    }

    /**
     * 向服务器上报设备信息并获取OTA响应 (保留原接口供以后可能的直接调用)
     */
    suspend fun reportDeviceAndGetOta(clientId: String, deviceId: String, otaUrl: String?): Result<OtaResponse> =
        otaNetRepository.reportDeviceAndGetOta(clientId, deviceId, otaUrl)
    
    /**
     * 创建设备上报请求数据
     */
    fun createDeviceReportRequest(clientId: String, deviceId: String): DeviceReportRequest =
        otaNetRepository.createDeviceReportRequest(clientId, deviceId)
}