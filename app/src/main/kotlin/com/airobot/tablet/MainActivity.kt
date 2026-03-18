package com.airobot.tablet

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.airobot.tablet.airobotui.AiRobotMainScreen
import com.airobot.tablet.airobotui.framework.theme.AiRobotTheme
import com.airobot.tablet.airobotui.framework.theme.RobotTheme
import com.airobot.tablet.airobotui.framework.theme.RobotThemeMode

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置全屏沉浸模式 - 隐藏系统状态栏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.apply {
            statusBarColor = android.graphics.Color.TRANSPARENT
            navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        
        // 隐藏状态栏
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        Log.d(TAG, "应用启动，开始初始化...")
        
        setContent {
            var themeMode by remember { mutableStateOf(RobotThemeMode.DARK) }
            
            AiRobotTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = RobotTheme.colors.background
                ) {
                    AiRobotMainScreen(
                        themeMode = themeMode,
                        onToggleTheme = {
                            themeMode = if (themeMode == RobotThemeMode.DARK) {
                                RobotThemeMode.LIGHT
                            } else {
                                RobotThemeMode.DARK
                            }
                            Log.d(TAG, "主题切换: $themeMode")
                        }
                    )
                }
            }
        }
    }
}
