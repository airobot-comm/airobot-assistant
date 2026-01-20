package com.airobotcomm.tablet

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.airobotcomm.tablet.ui.AiRobotServiceScreen
import com.airobotcomm.tablet.ui.theme.DarkColorScheme
import com.airobotcomm.tablet.ui.theme.YTheme

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
                    AiRobotServiceScreen()
                }
            }
        }
    }
}