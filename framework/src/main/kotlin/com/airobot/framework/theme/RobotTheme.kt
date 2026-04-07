package com.airobot.framework.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * AIRobot 主题模式
 */
enum class RobotThemeMode {
    LIGHT,
    DARK
}

/**
 * AIRobot 自定义主题颜色配置
 *
 * 包含所有 UI 组件需要的颜色 token，与 Material3 ColorScheme 并行使用。
 * Robot 角色本体颜色固定不变，不包含在此配置中。
 */
@Immutable
data class RobotThemeColors(
    // Background
    val background: Color,
    val backgroundGradientStart: Color,
    val backgroundGradientEnd: Color,
    val backgroundShapes: Color,

    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,

    // Accent
    val accent: Color,
    val accentBg: Color,

    // Card / Surface
    val cardBg: Color,
    val cardBorder: Color,
    val surfaceOverlay: Color,

    // Bubble
    val bubbleBg: Color,
    val bubbleBorder: Color,

    // Robot Aura (theme-dependent glow)
    val robotAuraStart: Color,
    val robotAuraEnd: Color,

    // Timer
    val timerBg: Color,
    val timerRingBg: Color,
    val timerRingActive: Color,
    val timerRingPaused: Color
)

/**
 * 深色主题颜色实例 — slate-900 基调
 */
val DarkRobotColors = RobotThemeColors(
    background = DarkBackground,
    backgroundGradientStart = DarkBackground,
    backgroundGradientEnd = DarkBackgroundEnd,
    backgroundShapes = DarkBackgroundShapes.copy(alpha = 0.4f),
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    accent = DarkAccent,
    accentBg = DarkAccentBg,
    cardBg = DarkCardBg.copy(alpha = 0.9f),
    cardBorder = DarkCardBorder,
    surfaceOverlay = DarkSurfaceOverlay,
    bubbleBg = DarkBubbleBg.copy(alpha = 0.4f),
    bubbleBorder = DarkBubbleBorder.copy(alpha = 0.5f),
    robotAuraStart = DarkRobotAuraStart.copy(alpha = 0.4f),
    robotAuraEnd = DarkRobotAuraEnd.copy(alpha = 0.4f),
    timerBg = DarkTimerBg,
    timerRingBg = DarkTimerRingBg,
    timerRingActive = DarkTimerRingActive,
    timerRingPaused = DarkTimerRingPaused
)

/**
 * 浅色主题颜色实例 — slate-200 基调
 */
val LightRobotColors = RobotThemeColors(
    background = LightBackground,
    backgroundGradientStart = LightBackground,
    backgroundGradientEnd = LightBackgroundEnd,
    backgroundShapes = LightBackgroundShapes.copy(alpha = 0.4f),
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted,
    accent = LightAccent,
    accentBg = LightAccentBg,
    cardBg = LightCardBg.copy(alpha = 0.9f),
    cardBorder = LightCardBorder,
    surfaceOverlay = LightSurfaceOverlay,
    bubbleBg = LightBubbleBg.copy(alpha = 0.4f),
    bubbleBorder = LightBubbleBorder.copy(alpha = 0.5f),
    robotAuraStart = LightRobotAuraStart.copy(alpha = 0.6f),
    robotAuraEnd = LightRobotAuraEnd.copy(alpha = 0.6f),
    timerBg = LightTimerBg,
    timerRingBg = LightTimerRingBg,
    timerRingActive = LightTimerRingActive,
    timerRingPaused = LightTimerRingPaused
)

/**
 * CompositionLocal 用于在 Compose 树中传递 RobotThemeColors
 */
val LocalRobotThemeColors = staticCompositionLocalOf { DarkRobotColors }

/**
 * CompositionLocal 用于在 Compose 树中传递当前主题模式
 */
val LocalRobotThemeMode = staticCompositionLocalOf { RobotThemeMode.DARK }

/**
 * 主题访问器 — 各组件通过 RobotTheme.colors 获取当前主题颜色
 *
 * Usage:
 *   val bg = RobotTheme.colors.background
 *   val mode = RobotTheme.mode
 */
object RobotTheme {
    val colors: RobotThemeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalRobotThemeColors.current

    val mode: RobotThemeMode
        @Composable
        @ReadOnlyComposable
        get() = LocalRobotThemeMode.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalRobotThemeMode.current == RobotThemeMode.DARK
}
