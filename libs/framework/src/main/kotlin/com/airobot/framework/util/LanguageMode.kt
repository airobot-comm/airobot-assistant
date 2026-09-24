package com.airobot.framework.util

import java.util.Locale

/**
 * AIRobot 语言模式
 */
enum class LanguageMode {
    SYSTEM,
    ENGLISH,
    CHINESE;

    fun toLocale(): Locale? {
        return when (this) {
            SYSTEM -> null
            ENGLISH -> Locale.ENGLISH
            CHINESE -> Locale.SIMPLIFIED_CHINESE
        }
    }

    fun getDisplayName(): String {
        return when (this) {
            SYSTEM -> "跟随系统"
            ENGLISH -> "English"
            CHINESE -> "简体中文"
        }
    }

    fun getDisplayNameEn(): String {
        return when (this) {
            SYSTEM -> "System"
            ENGLISH -> "English"
            CHINESE -> "Chinese"
        }
    }
}
