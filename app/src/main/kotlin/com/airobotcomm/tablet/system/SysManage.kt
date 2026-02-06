package com.airobotcomm.tablet.system

import com.airobotcomm.tablet.system.model.ActiveInfo
import com.airobotcomm.tablet.system.model.AiAgent
import com.airobotcomm.tablet.system.model.CommCredentials
import com.airobotcomm.tablet.system.model.DeviceInfo
import com.airobotcomm.tablet.system.model.SystemInfo
import kotlinx.coroutines.flow.StateFlow

/**
 * System Module State
 * Replaces the previous OtaState to provide a unified system state
 */
sealed class SysState {
    object Idle : SysState()
    object Checking : SysState()
    data class UpdateAvailable(val version: String, val url: String) : SysState()
    object DeviceActivationRequired : SysState()  // Device not activated
    data class AiRobotActivationRequired(val code: String) : SysState()  // AIRobot needs activation
    @Deprecated("Use AiRobotActivationRequired instead")
    data class ActivationRequired(val code: String) : SysState()
    object Ready : SysState() // Both device and AIRobot activated
    data class Error(val message: String) : SysState()
}

/**
 * System Management Service Interface
 * Provides unified system functionality: activation, credentials, info management
 */
interface SysManage {
    val state: StateFlow<SysState>
    val systemInfo: StateFlow<SystemInfo>

    // ===== Device-level APIs =====
    
    /**
     * Validate if productKey meets requirements (format, length, etc.)
     */
    suspend fun validateProductKey(productKey: String): Boolean

    /**
     * Activate device with productKey
     */
    suspend fun deviceActivate(productKey: String): Result<ActiveInfo>

    /**
     * Get device basic information (immutable)
     */
    suspend fun getDeviceInfo(): DeviceInfo

    /**
     * Get device activation status
     */
    suspend fun getDeviceActivation(): ActiveInfo

    /**
     * Check if device is activated
     */
    suspend fun isDeviceActivated(): Boolean

    // ===== AIRobot-level APIs =====

    /**
     * Configure AIRobot agent (URL, model, etc.)
     * Triggers OTA authentication to get activation code and credentials
     */
    suspend fun configureAiAgent(agentUrl: String, model: String = "qianwen3"): Result<AiAgent>

    /**
     * Confirm AIRobot activation with OTA-provided code and credentials
     */
    suspend fun confirmAiRobotActivation(code: String, credentials: CommCredentials): Result<AiAgent>

    /**
     * Get AIRobot agent information (including activation state and credentials)
     */
    suspend fun getAiAgent(): AiAgent

    /**
     * Get AIRobot activation code (from OTA)
     */
    suspend fun getAiRobotActivationCode(): String

    /**
     * Check if AIRobot is activated
     */
    suspend fun isAiRobotActivated(): Boolean

    /**
     * Get communication credentials from activated agent
     * Returns null if not activated
     */
    suspend fun getCommCredentials(): CommCredentials?

    // ===== Legacy/Backward Compatibility =====

    /**
     * Update system configuration
     */
    suspend fun updateSystemInfo(info: SystemInfo)

    /**
     * Get current system info
     * @deprecated Use specific getters instead
     */
    @Deprecated("Use getDeviceInfo() and getAiAgent() instead")
    suspend fun getSystemInfo(): SystemInfo

    /**
     * Get device info
     * @deprecated Use getDeviceInfo() instead
     */
    @Deprecated("Use getDeviceInfo() instead", ReplaceWith("getDeviceInfo()"))
    suspend fun getDevInfo(): DeviceInfo

    /**
     * Confirm activation with code
     * @deprecated Use confirmAiRobotActivation() instead
     */
    @Deprecated("Use confirmAiRobotActivation() instead")
    suspend fun airobotActivate(code: String)

    /**
     * Get communication credentials for Network Service
     * @deprecated Use getCommCredentials() instead
     */
    @Deprecated("Use getCommCredentials() instead", ReplaceWith("getCommCredentials()"))
    suspend fun commCredential(): CommCredentials
    
    /**
     * Start/Initialize system check (OTA, Activation)
     */
    fun start()
}
