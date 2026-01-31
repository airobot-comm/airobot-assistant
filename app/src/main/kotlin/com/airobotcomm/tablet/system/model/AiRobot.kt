package com.airobotcomm.tablet.system.model

import java.util.UUID

/**
 * Ai机器人配置信息 - 角色名字，id，能力与模型等配置信息
 */
data class AiRobot(
    val roleName: String = "小美",                      // activate airobot's role-name：角色别名
    val roleId: String = UUID.randomUUID().toString(), // activate role-uuid
    val mcpEnabled: Boolean = false
){
   // todo：完成初始的构造方法，自己先赋值
}