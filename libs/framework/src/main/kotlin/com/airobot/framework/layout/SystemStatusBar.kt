package com.airobot.framework.layout

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.os.BatteryManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.R
import com.airobot.framework.theme.RobotTheme
import com.airobot.framework.theme.StatusCyan
import com.airobot.framework.theme.StatusEmerald
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 系统状态栏组件 - 负责网络、电量等基础状态展示
 */
@Composable
fun SystemStatusBar(
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val tint = RobotTheme.colors.textMuted

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp), // 增加间距
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "错误",
                modifier = Modifier.size(18.dp),
                tint = Color(0xFFEF4444)
            )
        }

        BatteryLevelIcon(tint = tint)
        NetworkStatusIcon(tint = tint)
        TimeDisplay()
    }
}

@Composable
private fun TimeDisplay() {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Text(
        text = timeFormat.format(Date(currentTime)),
        color = RobotTheme.colors.textPrimary,
        fontSize = 22.sp, // 增大字体
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp
    )
}

@SuppressLint("MissingPermission")
@Composable
private fun NetworkStatusIcon(tint: Color) {
    val context = LocalContext.current
    var wifiConnected by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // 初始状态
        val activeNetwork = connectivityManager.activeNetwork
        wifiConnected = activeNetwork != null

        // 动态监听
        connectivityManager.registerDefaultNetworkCallback(object :
            ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                wifiConnected = true
            }

            override fun onLost(network: Network) {
                wifiConnected = false
            }
        })
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val statusColor = if (wifiConnected) RobotTheme.colors.textMuted else Color(0xFFEF4444).copy(alpha = 0.6f)
        Icon(
            painter = painterResource(id = if (wifiConnected) R.drawable.wifi else R.drawable.wifi_off),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = statusColor
        )
        Text(
            text = if (wifiConnected) "ONLINE" else "OFFLINE",
            color = statusColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )
    }
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val statusColor =
            if (batteryLevel <= 20 && !isCharging) Color(0xFFEF4444) else RobotTheme.colors.textMuted
        Text(
            text = "$batteryLevel%",
            color = statusColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = statusColor
        )
    }
}


