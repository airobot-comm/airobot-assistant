package com.airobotcomm.tablet.system.model

import java.util.UUID

/**
 * 系统配置数据类 - 包含可修改的系统设置
 */
data class SystemConfig(
    val otaUrl: String = "https://api.tenclass.net/xiaozhi/ota/",
    val roleId: String = UUID.randomUUID().toString(),      // activate role-uuid
    val roleName: String = "小美",                          // activate airobot's role-name：角色别名
    val mcpEnabled: Boolean = false,
    val activationCode: String = ""
)
