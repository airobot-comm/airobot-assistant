package com.airobot.airbot.domain.model

import android.content.Context
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Supported character rendering engine types.
 * Parsed from the persisted string in Character.characterType.
 */
enum class CharacterType {
    ANDROID_CANVAS,  // Aether robot (Compose Canvas drawn)
    RIVE_IP;         // Rive-based IP character

    companion object {
        fun fromString(value: String): CharacterType =
            entries.find { it.name == value } ?: ANDROID_CANVAS
    }
}

/**
 * Data class representing a Rive character config entry.
 */
data class RiveCharacterEntry(
    val name: String,
    val resourceName: String,
    val scale: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

/**
 * Configuration manager to dynamically load Rive characters mapping from static code config.
 */
object RiveCharacterConfig {
    fun getResourceForRole(context: Context, roleName: String?): Int {
        val config = getCharacterConfig(context, roleName)
        val resId = context.resources.getIdentifier(
            config.resourceName,
            "raw",
            context.packageName
        )
        return if (resId != 0) resId else context.resources.getIdentifier(
            "xin_xiao_ling",
            "raw",
            context.packageName
        )
    }

    fun getCharacterConfig(context: Context, roleName: String?): RiveCharacterEntry {
        // Find the matching Rive character from the unified list
        val defaultRobot =
            Character.DEFAULT_ROBOTS.find { it.roleName.equals(roleName, ignoreCase = true) }
                ?: Character.DEFAULT_ROBOTS.find { it.characterType == "RIVE_IP" }!!

        return RiveCharacterEntry(
            name = defaultRobot.roleName,
            resourceName = defaultRobot.riveResourceName ?: "xin_xiao_ling",
            scale = defaultRobot.riveScale,
            offsetX = defaultRobot.riveOffsetX,
            offsetY = defaultRobot.riveOffsetY
        )
    }
}

/**
 * Ai机器人信息 - 智能体配置，角色名字，id，agent配置信息；
 * 可以配置不同智能体的多个airobot角色
 */
@Serializable
data class Character(
    // airobot role
    val roleName: String = "Aether",
    val roleId: String = UUID.randomUUID().toString(), // airobotActivate role-uuid

    // character profile fields
    val characterType: String = "ANDROID_CANVAS",
    val personality: String = "",
    val voiceModel: String = "火山模型",
    val alias: String = "小叶",
    val wakeWord: String = "你好小叶",

    // Unified Rive configuration properties
    val riveResourceName: String? = null,
    val riveScale: Float = 1.0f,
    val riveOffsetX: Float = 0f,
    val riveOffsetY: Float = 0f
) {
    // Character initialized with default values

    companion object {
        val DEFAULT_ROBOTS = listOf(
            Character(
                roleName = "Aether",
                characterType = "ANDROID_CANVAS",
                personality = "科技智慧、温暖陪伴",
                voiceModel = "火山模型",
                alias = "小叶",
                wakeWord = "你好小叶"
            ),
            Character(
                roleName = "心小灵",
                characterType = "RIVE_IP",
                personality = "温暖治愈、积极乐观",
                voiceModel = "火山模型",
                alias = "小灵",
                wakeWord = "你好小灵",
                riveResourceName = "xin_xiao_ling",
                riveScale = 1.0f,
                riveOffsetX = 0f,
                riveOffsetY = 0f
            ),
            Character(
                roleName = "花小小",
                characterType = "RIVE_IP",
                personality = "创意无限、绘画达人",
                voiceModel = "火山模型",
                alias = "小小",
                wakeWord = "你好小小",
                riveResourceName = "hua_xiao_xiao",
                riveScale = 1.2f,
                riveOffsetX = 0f,
                riveOffsetY = 0f
            ),
            Character(
                roleName = "ai小白",
                characterType = "RIVE_IP",
                personality = "聪明伶俐、百事通",
                voiceModel = "火山模型",
                alias = "小白",
                wakeWord = "你好小白",
                riveResourceName = "robot_xiao_bai",
                riveScale = 1.4f,
                riveOffsetX = 5.0f,
                riveOffsetY = 5.0f
            )
        )
    }

    fun getWakeWords(): List<String> {
        val list = mutableListOf<String>()
        val cleanAlias = alias.trim()
        val cleanWakeWord = wakeWord.trim()
        if (cleanAlias.isNotEmpty()) {
            list.add(cleanAlias)
        }
        if (cleanWakeWord.isNotEmpty() && cleanWakeWord != cleanAlias) {
            list.add(cleanWakeWord)
        }
        return list
    }
}
