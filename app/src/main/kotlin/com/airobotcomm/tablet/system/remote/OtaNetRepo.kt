package com.airobotcomm.tablet.system.remote

/**
 * OTA仓库接口 - 定义OTA相关操作的契约
 */
interface OtaNetRepo {
    /**
     * 向服务器上报设备信息并获取OTA响应
     */
    suspend fun reportDeviceAndGetOta(clientId: String, deviceId: String, otaUrl: String?): Result<OtaResponse>

    /**
     * 创建设备上报请求数据
     */
    fun createDeviceReportRequest(clientId: String, deviceId: String): DeviceReportRequest
}