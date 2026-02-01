package com.airobotcomm.tablet.system

import com.airobotcomm.tablet.system.remote.OtaNetRepo
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
 * ota获取动态WebSocket 连接参数类 - 由 OtaManager 提供给 NetworkService
 */
data class CommParams(
    val deviceId: String,
    val macAddress: String,
    val clientId: String,
    val clientName: String,

    // websocket 连接参数
    val url: String,
    val token: String

    // mqtt 通信参数，待补充
)

/**
 * OTA 业务逻辑类 - 处理 OTA 相关的业务规则
 */
@Singleton
class OtaManager @Inject constructor(
    private val otaNetRepo: OtaNetRepo,
    private val systemManager: SystemManager
) {
    private val _state = MutableStateFlow<OtaState>(OtaState.Idle)
    val state: StateFlow<OtaState> = _state.asStateFlow()

    // 动态数据维护
    private var dynamicWsUrl: String = ""
    private var dynamicToken: String = ""

    /**
     * 获取 WebSocket 连接参数
     * 为 comm/network 模块提供访问 ota 获取的最新 ws url, token 等信息
     */
    suspend fun getWsCommParams(): CommParams {
        val info = systemManager.getSystemInfo()
        return CommParams(
            deviceId = systemManager.getDeviceId(),
            macAddress = systemManager.getMacAddress(),
            clientId = info.clientId,
            // Assuming roleName was part of config before, but now maybe part of stored info or fixed.
            // SystemConfig had roleName. SystemInfo has aiRobotArray.
            // Let's use the first robot's roleName if available, or default.
            clientName = info.aiRobotArray.firstOrNull()?.roleName ?: "AiRobot",

            // 从ota动态更新数据中获取新的ws连接参数
            url = dynamicWsUrl,
            token = dynamicToken
        )
    }

    /**
     * 获取 MQTT 连接参数
     * 为 comm/network 模块提供访问 ota 获取的最新 ws url, token 等信息
     */
    suspend fun getMQTTCommParams() {
     //todo 待完善，从服务器已经能获取，但comm层目前还没支持
    }

    /**
     * 检查更新和激活状态
     */
    suspend fun checkUpdateAndActivation() {
        _state.value = OtaState.Checking
        val info = systemManager.getSystemInfo()
        
        if (info.otaUrl.isBlank()) {
            _state.value = OtaState.Error("OTA URL is not configured")
            return
        }

        val result = otaNetRepo.reportDeviceAndGetOta(
            clientId = info.clientId,
            deviceId = systemManager.getMacAddress(),
            otaUrl = info.otaUrl
        )

        result.onSuccess { response ->
            // 更新动态数据
            dynamicWsUrl = response.websocket.url
            dynamicToken = response.websocket.token

            // 1. 检查激活状态
            val activationCode = response.activation?.code
            if (!activationCode.isNullOrEmpty()) {
                _state.value = OtaState.ActivationRequired(activationCode)
                return@onSuccess
            }

            _state.value = OtaState.Activated
        }.onFailure { e ->
            _state.value = OtaState.Error(e.message ?: "OTA check failed")
        }
    }

    /**
     * 确认激活，todo：待完善，新的激活方式需要再向服务器提交产品序列号信息等
     */
    suspend fun confirmActivation(code: String) {
        val currentInfo = systemManager.getSystemInfo()
        val updatedActiveInfo = currentInfo.activeInfo!!.copy(activationCode = code)
        systemManager.updateSystemInfo(currentInfo.copy(activeInfo = updatedActiveInfo))
        _state.value = OtaState.Activated
    }
    
    /**
     * 更新动态数据 (外部更新，如 ws 连接过程中获得新 token)
     */
    fun updateDynamicData(wsUrl: String? = null, token: String? = null) {
        wsUrl?.let { dynamicWsUrl = it }
        token?.let { dynamicToken = it }
    }
}