package com.airobot.assistant.airobotui.framework.statusbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.os.BatteryManager
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.airobot.assistant.R
import com.airobot.assistant.airobotui.framework.theme.RobotTextPrimary

/**
 * 系统状态栏组件 - 负责网络、电量等基础状态展示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemStatusBar(
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = RobotTextPrimary.copy(alpha = 0.3f)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip {
                        Text(errorMessage ?: "")
                    }
                },
                state = rememberTooltipState()
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "网络错误",
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFFEF4444)
                )
            }
        }
        
        NetworkStatusIcon(tint = tint)
        BatteryLevelIcon(tint = tint)
    }
}

@Composable
private fun NetworkStatusIcon(tint: Color) {
    val context = LocalContext.current
    var wifiConnected by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        // 初始状态
        val activeNetwork = connectivityManager.activeNetwork
        wifiConnected = activeNetwork != null
        
        // 动态监听
        connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                wifiConnected = true
            }
            
            override fun onLost(network: Network) {
                wifiConnected = false
            }
        })
    }
    
    Icon(
        painter = painterResource(id = if (wifiConnected) R.drawable.wifi else R.drawable.wifi_off),
        contentDescription = if (wifiConnected) "WiFi已连接" else "WiFi未连接",
        modifier = Modifier.size(16.dp),
        tint = if (wifiConnected) tint else Color(0xFFEF4444).copy(alpha = 0.3f)
    )
}

@Composable
private fun BatteryLevelIcon(tint: Color) {
    val context = LocalContext.current
    var batteryLevel by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }
    
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    batteryLevel = (level * 100 / scale.toFloat()).toInt()
                    
                    val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                 status == BatteryManager.BATTERY_STATUS_FULL
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
    
    val iconRes = when {
        isCharging -> R.drawable.battery_charging
        batteryLevel > 80 -> R.drawable.battery_full
        batteryLevel > 20 -> R.drawable.battery_medium
        else -> R.drawable.battery_low
    }
    
    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = "电量: $batteryLevel%",
        modifier = Modifier.size(16.dp),
        tint = if (batteryLevel <= 20 && !isCharging) Color(0xFFEF4444).copy(alpha = 0.3f) else tint
    )
}

