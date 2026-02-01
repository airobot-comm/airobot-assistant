package com.airobotcomm.tablet.system

import com.airobotcomm.tablet.system.model.CommCredentials
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
 * ota激活/服务器动态认证获取新的通信凭证，目前支持ws
 * todo：后续要支持MQTT，支持V2的动态认证激活模式
 */
data class CommCredentials(
    val deviceId: String,
    val macAddress: String,
    val clientId: String,

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
    private val sysManager: SysManager
) {
    private val _state = MutableStateFlow<OtaState>(OtaState.Idle)
    val state: StateFlow<OtaState> = _state.asStateFlow()

    // 动态数据维护
    private var dynamicWsUrl: String = ""
    private var dynamicToken: String = ""

    /**
     * 获取通信凭证：只有服务器激活/认证通过有效
     * 为 comm/network 模块提供动态的通信凭证
     */
    suspend fun commCredentials(): CommCredentials {
        val info = sysManager.getSystemInfo()
        return CommCredentials(
            deviceId = sysManager.getDeviceId(),
            macAddress = sysManager.getMacAddress(),
            clientId = info.clientId,

            // 从ota动态更新数据中获取新的ws连接参数
            url = dynamicWsUrl,
            token = dynamicToken
        )
    }

    /**
     * 检查更新和激活状态
     */
    suspend fun checkUpdateAndActivation() {
        _state.value = OtaState.Checking
        val info = sysManager.getSystemInfo()
        
        if (info.otaUrl.isBlank()) {
            _state.value = OtaState.Error("OTA URL is not configured")
            return
        }

        val result = otaNetRepo.reportDeviceAndGetOta(
            clientId = info.clientId,
            deviceId = sysManager.getMacAddress(),
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
        val currentInfo = sysManager.getSystemInfo()
        val updatedActiveInfo = currentInfo.activeInfo!!.copy(activationCode = code)
        sysManager.updateSystemInfo(currentInfo.copy(activeInfo = updatedActiveInfo))
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