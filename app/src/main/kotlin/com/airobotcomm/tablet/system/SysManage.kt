package com.airobotcomm.tablet.system

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
    data class ActivationRequired(val code: String) : SysState()
    object Ready : SysState() // Replaces Activated
    data class Error(val message: String) : SysState()
}

/**
 * System Management Service Interface
 * Provides unified system functionality: activation, credentials, info management
 */
interface SysManage {
    val state: StateFlow<SysState>

    /**
     * Confirm activation with code
     */
    suspend fun airobotActivate(code: String)

    /**
     * Get communication credentials for Network Service
     */
    suspend fun getCredential(): CommCredentials

    /**
     * Update system configuration
     */
    suspend fun updateSystemInfo(info: SystemInfo)

    /**
     * Get current system info
     */
    suspend fun getSystemInfo(): SystemInfo

    /**
     * Get device info
     */
    suspend fun getDevInfo(): DeviceInfo
    
    /**
     * Start/Initialize system check (OTA, Activation)
     * Replaces checkUpdateAndActivation, essentially the startup entry point
     */
    fun start()
}
