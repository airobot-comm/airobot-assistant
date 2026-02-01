package com.airobotcomm.tablet.system.model

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
 * 系统激活信息 - 应用系统激活密钥，激活返回码等信息
 */
data class ActiveInfo(
    val productKey: String,      // 产品激活密钥，授权给用户
    val secretKey: String,       // 简单的激活密钥生成与检测secretKey
    val serviceTime: String,     // 服务器激活时候的时间戳
    val serverCode: String = "",
    val activationCode: String = "" // Migrated activation code from SystemConfig
){

    fun isValid(): Boolean {
        // Simple check: productKey must not be empty.
        // TODO: Implement actual secret key verification logic
        return productKey.isNotEmpty()
    }
}