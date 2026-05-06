package com.airobot.core.comm.provider

data class CommDeviceInfo(
    val macAddress: String,
    val deviceId: String
)

data class CommAuthCredentials(
    val url: String,
    val token: String,
    val clientId: String
)

interface CommSysProvider {
    suspend fun getDeviceInfo(): CommDeviceInfo
    suspend fun getCommCredentials(): CommAuthCredentials?
    suspend fun isDeviceActivated(): Boolean
    suspend fun isAiRobotActivated(): Boolean
}
