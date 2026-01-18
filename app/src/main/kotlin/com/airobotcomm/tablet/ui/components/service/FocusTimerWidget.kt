package com.airobotcomm.tablet.ui.components.service

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobotcomm.tablet.R
import com.airobotcomm.tablet.ui.state.TimerCommand
import com.airobotcomm.tablet.ui.state.TimerStatus
import kotlinx.coroutines.delay

/**
 * 专注计时器组件
 * 
 * Web原型对应: FocusTimerWidget.tsx
 * 
 * 功能:
 * - 圆形进度环
 * - 数字倒计时
 * - 任务名称显示
 * - 完成/暂停/运行状态
 * - 完成庆祝动画
 */
@Composable
fun FocusTimerWidget(
    command: TimerCommand?,
    timerStatus: TimerStatus,
    onTimerComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var timeLeft by remember { mutableStateOf(0) }
    var totalTime by remember { mutableStateOf(0) }
    var isCompleted by remember { mutableStateOf(false) }
    var taskName by remember { mutableStateOf("等待指令...") }
    
    val infiniteTransition = rememberInfiniteTransition(label = "timerAnimation")
    
    // 初始化计时器
    LaunchedEffect(command, timerStatus) {
        if (command != null && timerStatus != TimerStatus.IDLE && totalTime == 0) {
            totalTime = command.duration
            timeLeft = command.duration
            taskName = command.task.ifEmpty { "专注时刻" }
            isCompleted = false
        }
    }
    
    // 计时逻辑
    LaunchedEffect(timerStatus, timeLeft) {
        if (timerStatus == TimerStatus.RUNNING && timeLeft > 0) {
            delay(1000L)
            timeLeft--
            if (timeLeft <= 0) {
                isCompleted = true
                onTimerComplete()
            }
        }
    }
    
    // 重置逻辑
    LaunchedEffect(timerStatus) {
        if (timerStatus == TimerStatus.IDLE && !isCompleted && totalTime > 0) {
            totalTime = 0
            timeLeft = 0
        }
    }
    
    val progress = if (totalTime > 0) timeLeft.toFloat() / totalTime else 0f
    val isRunning = timerStatus == TimerStatus.RUNNING
    val isPaused = timerStatus == TimerStatus.PAUSED
    
    // 颜色
    val gradientColors = when {
        isCompleted -> listOf(Color(0xFF10B981), Color(0xFF14B8A6)) // emerald-teal
        isPaused -> listOf(Color(0xFFF59E0B), Color(0xFFF97316)) // amber-orange
        else -> listOf(Color(0xFFEF4444), Color(0xFFF97316)) // red-orange
    }
    
    val progressColor = when {
        isCompleted -> Color(0xFF10B981) // emerald-500
        isPaused -> Color(0xFFEAB308) // yellow-500
        else -> Color(0xFFEF4444) // red-500
    }
    
    val taskColor = when {
        isCompleted -> Color(0xFF6EE7B7) // emerald-200
        isPaused -> Color(0xFFFDE68A) // yellow-200
        else -> Color(0xFFFCA5A5) // red-200
    }
    
    // 完成动画
    val completedScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCompleted) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "completedScale"
    )
    
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 计时器主体
        Box(
            modifier = Modifier
                .size(220.dp)
                .scale(if (isCompleted) completedScale else 1f),
            contentAlignment = Alignment.Center
        ) {
            // 外圈渐变背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(colors = gradientColors)
                    )
            )
            
            // 光泽反射
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .offset(y = (-30).dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
                    .blur(2.dp)
            )
            
            // 内圈
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A).copy(alpha = 0.9f)), // slate-900
                contentAlignment = Alignment.Center
            ) {
                // 完成爆发效果
                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF10B981).copy(alpha = 0.2f))
                            .blur(20.dp)
                    )
                }
                
                // 进度环
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    val strokeWidth = 20f
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = center
                    
                    // 背景轨道
                    drawCircle(
                        color = Color(0xFF333333),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )
                    
                    // 进度弧
                    if (isRunning || isPaused || isCompleted) {
                        drawArc(
                            color = progressColor,
                            startAngle = -90f,
                            sweepAngle = 360f * (1 - progress),
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            topLeft = center - androidx.compose.ui.geometry.Offset(radius, radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                        )
                    }
                }
                
                // 刻度线
                repeat(12) { index ->
                    Box(
                        modifier = Modifier
                            .size(4.dp, 8.dp)
                            .offset(y = (-85).dp)
                            .graphicsLayer(
                                rotationZ = index * 30f,
                                transformOrigin = TransformOrigin(0.5f, 11f)
                            )
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                }
                
                // 数字/完成图标
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedContent(
                        targetState = isCompleted,
                        transitionSpec = {
                            scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut()
                        },
                        label = "timerContent"
                    ) { completed ->
                        if (completed) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = Color(0xFF34D399) // emerald-400
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "FINISHED",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                            }
                        } else {
                            Text(
                                text = formatTime(timeLeft),
                                color = Color.White,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                    
                    // 任务名称
                    AnimatedContent(
                        targetState = taskName,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "taskName"
                    ) { name ->
                        Text(
                            text = name.take(10),
                            color = taskColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            
            // 顶部按钮装饰
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-8).dp)
                    .size(40.dp, 24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when {
                            isCompleted -> Color(0xFF047857) // emerald-700
                            isPaused -> Color(0xFFCA8A04) // yellow-600
                            else -> Color(0xFFB91C1C) // red-700
                        }
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 状态指示
        AnimatedContent(
            targetState = when {
                !isRunning && !isPaused && !isCompleted -> "WAITING"
                isRunning -> "RUNNING"
                isPaused -> "PAUSED"
                isCompleted -> "COMPLETED"
                else -> "WAITING"
            },
            transitionSpec = {
                fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically()
            },
            label = "statusIndicator"
        ) { status ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (status) {
                    "WAITING" -> {
                        Icon(
                            painter = painterResource(id = R.drawable.timer),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "WAITING FOR COMMAND",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    "RUNNING" -> {
                        // 脉冲圆点
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(500),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulseAlpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = pulseAlpha))
                        )
                        Text(
                            text = "FOCUS MODE ACTIVE",
                            color = Color(0xFFF87171), // red-400
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    "PAUSED" -> {
                        Icon(
                            painter = painterResource(id = R.drawable.volume_low), // pause icon
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFFACC15) // yellow-400
                        )
                        Text(
                            text = "TIMER PAUSED",
                            color = Color(0xFFFACC15),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    "COMPLETED" -> {
                        Icon(
                            painter = painterResource(id = R.drawable.star),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF34D399) // emerald-400
                        )
                        Text(
                            text = "GREAT JOB!",
                            color = Color(0xFF34D399),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.star),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF34D399)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 格式化时间 MM:SS
 */
private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
