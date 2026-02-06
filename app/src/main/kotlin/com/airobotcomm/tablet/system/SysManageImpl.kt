package com.airobotcomm.tablet.system

import android.content.Context
import com.airobotcomm.tablet.system.model.ActiveInfo
import com.airobotcomm.tablet.system.model.AiAgent
import com.airobotcomm.tablet.system.model.CommCredentials
import com.airobotcomm.tablet.system.model.DeviceInfo
import com.airobotcomm.tablet.system.model.SystemInfo
import com.airobotcomm.tablet.system.repository.SysInfoRepo
import com.airobotcomm.tablet.system.remote.OtaNetRepo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * System Management Implementation
 * Implements SysManage interface, consolidating System Config and OTA/Activation logic.
 */
@Singleton
class SysManageImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sysInfoRepo: SysInfoRepo,
    private val otaNetRepo: OtaNetRepo
) : SysManage {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()
    
    // Cache the system info in memory
    private var _systemInfo: SystemInfo? = null

    private val _state = MutableStateFlow<SysState>(SysState.Idle)
    override val state: StateFlow<SysState> = _state.asStateFlow()

    private val _systemInfoFlow = MutableStateFlow(SystemInfo())
    override val systemInfo: StateFlow<SystemInfo> = _systemInfoFlow.asStateFlow()

    // Dynamic data from OTA/Network
    private var dynamicWsUrl: String = ""
    private var dynamicToken: String = ""

    /**
     * Start/Initialize system check (OTA, Activation)
     */
    override fun start() {
        scope.launch {
            checkUpdateAndActivation()
        }
    }

    private suspend fun checkUpdateAndActivation() {
        _state.value = SysState.Checking
        val info = getSystemInfo() // Ensure loaded
        
        // 1. Check device activation first
        if (!info.deviceInfo.activation.isActivated) {
            _state.value = SysState.DeviceActivationRequired
            return
        }
        
        // 2. Check if agent URL is configured
        if (info.serviceUrl.isBlank()) {
            _state.value = SysState.Error("OTA URL is not configured")
            return
        }

        // 3. Perform OTA check for AIRobot activation
        val result = otaNetRepo.reportDeviceAndGetOta(
            clientId = info.clientId,
            deviceId = getMacAddress(), // Using MacAddress as deviceId for OTA as per previous logic
            otaUrl = info.serviceUrl
        )

        result.onSuccess { response ->
            // Extract credentials from OTA response
            val credentials = CommCredentials(
                url = response.websocket.url,
                token = response.websocket.token,
                clientId = info.clientId,
                topic = "",  // TODO: Extract from response when available
                qos = 0      // TODO: Extract from response when available
            )

            // Check if activation code is provided
            val activationCode = response.activation?.code
            if (!activationCode.isNullOrEmpty()) {
                // Auto-confirm activation with code and credentials
                confirmAiRobotActivation(activationCode, credentials)
                _state.value = SysState.AiRobotActivationRequired(activationCode)
                return@onSuccess
            }

            // If no activation code but has credentials, update agent
            val updatedAgent = info.aiAgent.copy(commCredentials = credentials)
            updateSystemInfo(info.copy(aiAgent = updatedAgent))
            _state.value = SysState.Ready
        }.onFailure { e ->
            _state.value = SysState.Error(e.message ?: "OTA check failed")
        }
    }

    // ===== Device-level APIs =====

    override suspend fun validateProductKey(productKey: String): Boolean {
        // Simple validation: non-empty and minimum length
        return productKey.isNotEmpty() && productKey.length >= 8
    }

    override suspend fun deviceActivate(productKey: String): Result<ActiveInfo> {
        if (!validateProductKey(productKey)) {
            return Result.failure(Exception("Invalid product key"))
        }

        // TODO: Implement actual secret key generation/validation with server
        val activation = ActiveInfo(
            productKey = productKey,
            secretKey = "stub_secret_${System.currentTimeMillis()}", // Stub
            time = System.currentTimeMillis().toString(),
            isActivated = true
        )

        // Persist activation
        val currentInfo = getSystemInfo()
        val updatedInfo = currentInfo.copy(
            deviceInfo = currentInfo.deviceInfo.copy(activation = activation)
        )
        updateSystemInfo(updatedInfo)

        // After device activation, trigger check for AIRobot activation
        scope.launch { checkUpdateAndActivation() }

        return Result.success(activation)
    }

    override suspend fun getDeviceInfo(): DeviceInfo {
        return ensureSystemInfo().deviceInfo
    }

    override suspend fun getDeviceActivation(): ActiveInfo {
        return getDeviceInfo().activation
    }

    override suspend fun isDeviceActivated(): Boolean {
        return getDeviceActivation().isActivated
    }

    // ===== AIRobot-level APIs =====

    override suspend fun configureAiAgent(agentUrl: String, model: String): Result<AiAgent> {
        val currentInfo = getSystemInfo()
        val updatedAgent = currentInfo.aiAgent.copy(
            agentUrl = agentUrl,
            model = model
        )
        updateSystemInfo(currentInfo.copy(aiAgent = updatedAgent))

        // Trigger OTA to attempt activation with new config
        scope.launch { checkUpdateAndActivation() }

        return Result.success(updatedAgent)
    }

    override suspend fun confirmAiRobotActivation(code: String, credentials: CommCredentials): Result<AiAgent> {
        val currentInfo = getSystemInfo()
        val activatedAgent = currentInfo.aiAgent.copy(
            activationCode = code,
            commCredentials = credentials
        )
        updateSystemInfo(currentInfo.copy(aiAgent = activatedAgent))
        
        // Update credentials in memory cache if needed, though they are in SystemInfo now
        dynamicWsUrl = credentials.url
        dynamicToken = credentials.token
        
        _state.value = SysState.Ready
        return Result.success(activatedAgent)
    }

    override suspend fun getAiAgent(): AiAgent {
        return ensureSystemInfo().aiAgent
    }

    override suspend fun getAiRobotActivationCode(): String {
        return getAiAgent().activationCode
    }

    override suspend fun isAiRobotActivated(): Boolean {
        val agent = getAiAgent()
        return agent.activationCode.isNotEmpty() && agent.commCredentials != null
    }

    override suspend fun getCommCredentials(): CommCredentials? {
        return getAiAgent().commCredentials
    }

    // ===== Legacy/Backward Compatibility Implementations =====

    @Deprecated("Use confirmAiRobotActivation instead")
    override suspend fun airobotActivate(code: String) {
        // Redirect to confirmAiRobotActivation with cached credentials if available
        // Note: This is legacy path, ideally shouldn't be called without credentials
        val agent = getAiAgent()
        val credentials = agent.commCredentials ?: CommCredentials(
            url = dynamicWsUrl,
            token = dynamicToken,
            clientId = ensureSystemInfo().clientId,
            topic = "",
            qos = 0
        )
        confirmAiRobotActivation(code, credentials)
    }

    @Deprecated("Use getCommCredentials instead")
    override suspend fun commCredential(): CommCredentials {
        return getCommCredentials() ?: CommCredentials(dynamicWsUrl, dynamicToken, "", "", 0)
    }

    @Deprecated("Use getDeviceInfo() instead", ReplaceWith("getDeviceInfo()"))
    override suspend fun getDevInfo(): DeviceInfo {
        return getDeviceInfo()
    }

    // --- Internal Helpers & SysInfo Management ---

    private suspend fun ensureSystemInfo(): SystemInfo {
        if (_systemInfo == null) {
            var sInfo = sysInfoRepo.loadConfig()
            
            // Check if initialization is needed (empty device Id)
            if (sInfo.deviceInfo.deviceId.isEmpty()) {
                 val deviceInfo = DeviceInfo.create(context)
                 
                 // Re-construct SystemInfo with initialized data
                 // deviceInfo.create already handles default empty activation
                 sInfo = sInfo.copy(deviceInfo = deviceInfo)
                 
                 // Save the initialized config
                 sysInfoRepo.saveConfig(sInfo)
            }
            _systemInfo = sInfo
            _systemInfoFlow.value = sInfo
        }
        return _systemInfo!!
    }

    suspend fun getDeviceId(): String = ensureSystemInfo().deviceInfo.deviceId
    suspend fun getMacAddress(): String = ensureSystemInfo().deviceInfo.macAddress
    
    // Exposed via Interface
    override suspend fun updateSystemInfo(info: SystemInfo) = mutex.withLock {
        sysInfoRepo.saveConfig(info)
        _systemInfo = info
        _systemInfoFlow.value = info
        // If critical info changed, arguably we should restart check, but depends on requirement.
        // For now, simple update.
    }

    @Deprecated("Use systemInfo property or individual getters instead")
    override suspend fun getSystemInfo(): SystemInfo = mutex.withLock {
        ensureSystemInfo()
    }
}
