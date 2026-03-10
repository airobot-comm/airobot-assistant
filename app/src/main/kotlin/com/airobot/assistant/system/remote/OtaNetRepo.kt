package com.airobot.assistant.system.remote

/**
 * OTA仓库接口 - 定义OTA相关操作的契约
 * todo ：后续优化，改为通用的系统接口，xiaozhi的服务是agent，迁移到agent—auth
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
