package com.airobotcomm.tablet.domain.ota

import com.airobotcomm.tablet.domain.model.OtaResponse
import com.airobotcomm.tablet.domain.model.DeviceReportRequest
import com.airobotcomm.tablet.domain.repository.OtaRepository
import javax.inject.Inject

/**
 * OTA业务逻辑类 - 处理OTA相关的业务规则
 */
class OtaManager @Inject constructor(
    private val otaRepository: OtaRepository
) {
    /**
     * 向服务器上报设备信息并获取OTA响应
     */
    suspend fun reportDeviceAndGetOta(clientId: String, deviceId: String, otaUrl: String?): Result<OtaResponse> = 
        otaRepository.reportDeviceAndGetOta(clientId, deviceId, otaUrl)
    
    /**
     * 创建设备上报请求数据
     */
    fun createDeviceReportRequest(clientId: String, deviceId: String): DeviceReportRequest = 
        otaRepository.createDeviceReportRequest(clientId, deviceId)
}