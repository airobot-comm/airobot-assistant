package com.airobot.core.system

import com.airobot.core.comm.provider.CommAuthCredentials
import com.airobot.core.comm.provider.CommDeviceInfo
import com.airobot.core.comm.provider.CommSysProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommSysProviderImpl @Inject constructor(
    private val sysManage: SysManage
) : CommSysProvider {
    override suspend fun getDeviceInfo(): CommDeviceInfo {
        val device = sysManage.getDeviceInfo()
        return CommDeviceInfo(
            macAddress = device.macAddress,
            deviceId = device.deviceId
        )
    }

    override suspend fun getCommCredentials(): CommAuthCredentials? {
        val creds = sysManage.getCommCredentials() ?: return null
        return CommAuthCredentials(
            url = creds.url,
            token = creds.token,
            clientId = creds.clientId
        )
    }

    override suspend fun isDeviceActivated(): Boolean {
        return sysManage.isDeviceActivated()
    }

    override suspend fun isAiRobotActivated(): Boolean {
        return sysManage.isAiRobotActivated()
    }
}
