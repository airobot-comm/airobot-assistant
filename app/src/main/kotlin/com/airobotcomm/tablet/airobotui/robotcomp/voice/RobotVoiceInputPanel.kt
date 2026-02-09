package com.airobotcomm.tablet.airobotui.robotcomp.voice

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobotcomm.tablet.R
import com.airobotcomm.tablet.airobotui.state.RobotVisualState
import com.airobotcomm.tablet.airobotui.state.TimerStatus

/**
 * 机器人风格语音输入面板
 * 
 * Web原型对应: VoiceInputPanel.tsx
 * 
 * 功能:
 * - 空闲状态：麦克风按钮 + 提示文字
 * - 聆听状态：波形动画 + 状态提示
 * - 思考状态：加载动画
 * - 说话状态：播放指示
 * - 专注模式：计时器控制按钮
 */
@Composable
fun RobotVoiceInputPanel(
    robotState: RobotVisualState,
    isConnected: Boolean,
    timerStatus: TimerStatus,
    audioLevel: Float = 0.0f,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onTimerControl: (String) -> Unit, // "PAUSE", "RESUME", "STOP"
    modifier: Modifier = Modifier
) {
    val isListening = robotState == RobotVisualState.LISTENING
    val isThinking = robotState == RobotVisualState.THINKING
    val isSpeaking = robotState == RobotVisualState.SPEAKING
    val isTimerActive = timerStatus != TimerStatus.IDLE
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedContent(
            targetState = when {
                isTimerActive -> "TIMER"
                robotState == RobotVisualState.IDLE -> "IDLE"
                else -> "ACTIVE"
            },
            transitionSpec = {
                fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
            },
            label = "panelContent"
        ) { state ->
            when (state) {
                "IDLE" -> IdleMicButton(
                    isConnected = isConnected,
                    onStartListening = onStartListening
                )
                "TIMER" -> TimerControlPanel(
                    timerStatus = timerStatus,
                    onTimerControl = onTimerControl
                )
                "ACTIVE" -> ActiveStatusPanel(
                    isListening = isListening,
                    isThinking = isThinking,
                    isSpeaking = isSpeaking,
                    audioLevel = audioLevel,
                    onStopListening = onStopListening
                )
            }
        }
    }
}

/**
 * 空闲状态麦克风按钮
 */
@Composable
private fun IdleMicButton(
    isConnected: Boolean,
    onStartListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f, // 增加波动范围
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAlpha"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp) // 增加间距
    ) {
        // 麦克风按钮 - 增大尺寸
        Box(
            modifier = Modifier.size(120.dp), // 从 80 增大到 120
            contentAlignment = Alignment.Center
        ) {
            // 脉冲环
            if (isConnected) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(ringScale)
                        .clip(CircleShape)
                        .background(Color(0xFF6366F1).copy(alpha = ringAlpha))
                )
            }
            
            // 按钮主体
            Box(
                modifier = Modifier
                    .size(100.dp) // 从 72 增大到 100
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isConnected) {
                                listOf(
                                    Color(0xFF0F172A),
                                    Color(0xFF1E293B)
                                )
                            } else {
                                listOf(
                                    Color(0xFF374151),
                                    Color(0xFF4B5563)
                                )
                            }
                        )
                    )
                    // .clickable(enabled = isConnected) { onStartListening() } // 移除点击触发
                    ,
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.mic),
                    contentDescription = "语音输入",
                    modifier = Modifier.size(44.dp), // 图标同步增大
                    tint = if (isConnected) Color.White else Color.White.copy(alpha = 0.5f)
                )
            }
        }
        
        // 提示文字 - 保持常驻
        VoiceHintText(
            text = if (isConnected) "呼唤\"Hi Robot\"开始对话" else "等待连接..."
        )
    }
}

/**
 * 统一的语音提示文字逻辑
 */
@Composable
private fun VoiceHintText(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.chat),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color(0xFF22D3EE)
        )
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}

/**
 * 活跃状态面板
 */
@Composable
private fun ActiveStatusPanel(
    isListening: Boolean,
    isThinking: Boolean,
    isSpeaking: Boolean,
    audioLevel: Float,
    onStopListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "activeStatus")
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .padding(horizontal = 32.dp, vertical = 20.dp) // 增大内边距
                .clickable(enabled = isListening) { onStopListening() },
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                isListening -> {
                        VoiceWaveform(
                            isActive = true,
                            barColor = Color(0xFF22D3EE),
                            audioLevel = audioLevel
                        )
                        Text(
                            text = "倾听中",
                            color = Color(0xFF22D3EE),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    }
                isThinking -> {
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing)
                        ),
                        label = "thinkingRotation"
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.settings),
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { rotationZ = rotation },
                        tint = Color(0xFF6366F1)
                    )
                    Text(
                        text = "思考中",
                        color = Color(0xFF6366F1),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
                isSpeaking -> {
                    SpeakingDots(
                        dotColor = Color.White
                    )
                    Text(
                        text = "正在播报",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}

/**
 * 计时器控制面板
 */
@Composable
private fun TimerControlPanel(
    timerStatus: TimerStatus,
    onTimerControl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "timerControl")
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 沙漏图标
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (timerStatus == TimerStatus.PAUSED) 
                        Color(0xFF1E293B).copy(alpha = 0.8f) 
                    else 
                        Color(0xFF0F172A).copy(alpha = 0.8f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (timerStatus == TimerStatus.RUNNING) {
                // 旋转边框
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing)
                    ),
                    label = "borderRotation"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = rotation }
                        .clip(CircleShape)
                        .background(Color.Transparent)
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.timer),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = if (timerStatus == TimerStatus.PAUSED) 
                    Color.White.copy(alpha = 0.3f) 
                else 
                    Color(0xFFA5F3FC).copy(alpha = 0.5f) // cyan-200
            )
        }
        
        // 控制按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 暂停/继续按钮
            if (timerStatus == TimerStatus.RUNNING) {
                TimerControlChip(
                    iconResId = R.drawable.volume_low, // pause icon
                    text = "暂停计时",
                    iconColor = Color(0xFFFACC15), // yellow-400
                    onClick = { onTimerControl("PAUSE") }
                )
            } else if (timerStatus == TimerStatus.PAUSED) {
                TimerControlChip(
                    iconResId = R.drawable.mic, // play icon
                    text = "继续计时",
                    iconColor = Color(0xFF34D399), // emerald-400
                    onClick = { onTimerControl("RESUME") }
                )
            }
            
            // 停止按钮
            TimerControlChip(
                iconResId = R.drawable.close, // stop icon
                text = "结束专注",
                iconColor = Color(0xFFF87171), // red-400
                isDestructive = true,
                onClick = { onTimerControl("STOP") }
            )
        }
    }
}

/**
 * 计时器控制按钮
 */
@Composable
private fun TimerControlChip(
    iconResId: Int,
    text: String,
    iconColor: Color,
    isDestructive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = iconColor
        )
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
