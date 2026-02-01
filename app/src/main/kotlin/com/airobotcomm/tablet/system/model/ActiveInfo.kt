package com.airobotcomm.tablet.system.model

import javax.crypto.SecretKey

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