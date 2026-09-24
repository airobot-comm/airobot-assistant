package com.airobot.framework.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.airobot.framework.util.LanguageMode

/**
 * Material3 深色配色方案
 */
val DarkColorScheme = darkColorScheme(
    primary = StatusCyan,
    secondary = DarkAccentBg,
    tertiary = StatusCyan,
    background = DarkBackground,
    surface = DarkCardBg,
    surfaceVariant = DarkCardBg,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkTextSecondary,
    error = StatusRed
)

/**
 * Material3 浅色配色方案
 */
val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    secondary = LightAccentBg,
    tertiary = LightAccent,
    background = LightBackground,
    surface = LightCardBg,
    surfaceVariant = Color(0xFFF5F5F5),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    outline = LightTextMuted,
    error = StatusRed
)

/**
 * AIRobot 统一主题入口
 *
 * 同时提供 Material3 MaterialTheme 和自定义 RobotTheme，
 * 各组件可选用 MaterialTheme.colorScheme 或 RobotTheme.colors。
 *
 * @param themeMode 主题模式 (LIGHT / DARK)
 * @param languageMode 语言模式 (SYSTEM / ENGLISH / CHINESE)
 */
@Composable
fun AiRobotTheme(
    themeMode: RobotThemeMode = RobotThemeMode.DARK,
    languageMode: LanguageMode = LanguageMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = themeMode == RobotThemeMode.DARK
    val materialScheme = if (isDark) DarkColorScheme else LightColorScheme
    val robotColors = if (isDark) DarkRobotColors else LightRobotColors

    // 处理语言切换
    val context = LocalContext.current
    val targetLocale = languageMode.toLocale()

    @SuppressLint("LocalContextConfigurationRead")
    val wrappedContext = remember(context, targetLocale) {
        if (targetLocale != null) {
            val config = Configuration(context.resources.configuration)
            config.setLocale(targetLocale)

            // 使用 ContextWrapper 包装原始 Context，以便 Hilt 等库仍能通过 unwrapping 找到 Activity
            object : ContextWrapper(context) {
                private val localizedContext = context.createConfigurationContext(config)
                override fun getResources() = localizedContext.resources
                override fun getAssets() = localizedContext.assets
            }
        } else {
            context
        }
    }

    // 动态更新状态栏颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = robotColors.background.toArgb()
            window.navigationBarColor = robotColors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalRobotThemeColors provides robotColors,
        LocalRobotThemeMode provides themeMode,
        LocalContext provides wrappedContext
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = Typography,
            content = content
        )
    }
}

// Keep legacy alias for backward compatibility during migration
@Composable
fun YTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AiRobotTheme(
        themeMode = if (darkTheme) RobotThemeMode.DARK else RobotThemeMode.LIGHT,
        content = content
    )
}
