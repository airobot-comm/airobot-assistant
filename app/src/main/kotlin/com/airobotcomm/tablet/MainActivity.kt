package com.airobotcomm.tablet

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.airobotcomm.tablet.ui.ConversationScreen
import com.airobotcomm.tablet.ui.screens.RobotConversationScreen
import com.airobotcomm.tablet.ui.theme.DarkColorScheme
import com.airobotcomm.tablet.ui.theme.YTheme

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
        
        // 切换UI模式：true = 新版机器人UI，false = 旧版对话UI
        private const val USE_ROBOT_UI = true
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置全屏沉浸模式 - 隐藏系统状态栏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.apply {
            statusBarColor = android.graphics.Color.TRANSPARENT
            navigationBarColor = android.graphics.Color.parseColor("#0F172A") // slate-900
        }
        
        // 隐藏状态栏
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        Log.d(TAG, "应用启动，开始初始化...")
        
        setContent {
            YTheme(
                darkTheme = true
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkColorScheme.background
                ) {
                    if (USE_ROBOT_UI) {
                        // 新版机器人角色对话UI
                        RobotConversationScreen()
                    } else {
                        // 旧版对话UI
                        ConversationScreen()
                    }
                }
            }
        }
    }
}