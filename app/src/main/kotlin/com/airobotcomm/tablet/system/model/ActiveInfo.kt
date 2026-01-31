package com.airobotcomm.tablet.system.model

import javax.crypto.SecretKey

/**
 * 系统激活信息 - 应用系统激活密钥，激活返回码等信息
 */
data class ActiveInfo(
    val productKey: String,      // 产品激活密钥，授权给用户
    val secretKey: String,       // 简单的激活密钥生成与检测secretKey
    val serviceTime: String,     // 服务器激活时候的时间戳
    val serverCode: String = ""
){
    // todo：完成构造方法，

   // todo：productKey的检测功能（设计一个私有密钥key）
}