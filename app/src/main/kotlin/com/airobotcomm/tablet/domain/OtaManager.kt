package com.airobotcomm.tablet.domain

import com.airobotcomm.tablet.domain.repository.OtaNetRepo
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
    private val otaNetRepo: OtaNetRepo,
    private val systemConfig: SystemConfig
) {
    private val _state = MutableStateFlow<OtaState>(OtaState.Idle)
    val state: StateFlow<OtaState> = _state.asStateFlow()

    /**
     * 检查更新和激活状态
     */
    suspend fun checkUpdateAndActivation() {
        _state.value = OtaState.Checking
        val config = systemConfig.loadConfig()
        
        if (config.otaUrl.isBlank()) {
            _state.value = OtaState.Error("OTA URL is not configured")
            return
        }

        val result = otaNetRepo.reportDeviceAndGetOta(
            clientId = config.clientId,
            deviceId = config.deviceId,
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
        val currentConfig = systemConfig.loadConfig()
        systemConfig.saveConfig(currentConfig.copy(activationCode = code))
        _state.value = OtaState.Activated

        // todo: 发送激活请求(目前服务器不支持，人工激活方式)
    }
}