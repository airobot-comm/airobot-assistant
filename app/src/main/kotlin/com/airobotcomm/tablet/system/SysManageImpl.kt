package com.airobotcomm.tablet.system

import android.content.Context
import com.airobotcomm.tablet.system.model.CommCredentials
import com.airobotcomm.tablet.system.model.DeviceInfo
import com.airobotcomm.tablet.system.model.SystemInfo
import com.airobotcomm.tablet.system.model.ActiveInfo
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
        
        if (info.serviceUrl.isBlank()) {
            _state.value = SysState.Error("OTA URL is not configured")
            return
        }

        val result = otaNetRepo.reportDeviceAndGetOta(
            clientId = info.clientId,
            deviceId = getMacAddress(), // Using MacAddress as deviceId for OTA as per previous logic
            otaUrl = info.serviceUrl
        )

        result.onSuccess { response ->
            // Update dynamic data
            dynamicWsUrl = response.websocket.url
            dynamicToken = response.websocket.token

            // 1. Check activation
            val activationCode = response.activation?.code
            if (!activationCode.isNullOrEmpty()) {
                _state.value = SysState.ActivationRequired(activationCode)
                return@onSuccess
            }

            _state.value = SysState.Ready
        }.onFailure { e ->
            _state.value = SysState.Error(e.message ?: "OTA check failed")
        }
    }

    override suspend fun airobotActivate(code: String) {
        val currentInfo = getSystemInfo()
        val updatedAgentInfo = currentInfo.aiAgent!!.copy(activationCode = code)
        updateSystemInfo(currentInfo.copy(aiAgent = updatedAgentInfo))
        _state.value = SysState.Ready
    }

    override suspend fun getCredential(): CommCredentials {
        val info = getSystemInfo()
        return CommCredentials(
            deviceId = getDeviceId(),
            macAddress = getMacAddress(),
            clientId = info.clientId,
            url = dynamicWsUrl,
            token = dynamicToken
        )
    }

    override suspend fun getDevInfo(): DeviceInfo {
        return ensureSystemInfo().deviceInfo!!
    }

    // --- Internal Helpers & SysInfo Management ---

    private suspend fun ensureSystemInfo(): SystemInfo {
        if (_systemInfo == null) {
            var sInfo = sysInfoRepo.loadConfig()
            
            // Check if initialization is needed (empty device Id)
            if (sInfo.deviceInfo!!.deviceId.isEmpty()) {
                 val deviceInfo = DeviceInfo.create(context)
                 
                 // Re-construct SystemInfo with initialized data
                 sInfo = sInfo.copy(
                     deviceInfo = deviceInfo,
                     // Ensure active info is also present if default was empty
                     activeInfo = if(sInfo.activeInfo!!.productKey.isEmpty())
                         ActiveInfo(productKey="", secretKey="", time="") else sInfo.activeInfo
                 )
                 
                 // Save the initialized config
                 sysInfoRepo.saveConfig(sInfo)
            }
            _systemInfo = sInfo
        }
        return _systemInfo!!
    }

    suspend fun getDeviceId(): String = ensureSystemInfo().deviceInfo!!.deviceId
    suspend fun getMacAddress(): String = ensureSystemInfo().deviceInfo!!.macAddress
    
    // Exposed via Interface
    override suspend fun updateSystemInfo(info: SystemInfo) = mutex.withLock {
        sysInfoRepo.saveConfig(info)
        _systemInfo = info
        // If critical info changed, arguably we should restart check, but depends on requirement.
        // For now, simple update.
    }

    override suspend fun getSystemInfo(): SystemInfo = mutex.withLock {
        ensureSystemInfo()
    }
}
