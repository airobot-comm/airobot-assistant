package com.airobot.tablet.airobotui.comp.voice

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
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.border
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.R
import com.airobot.tablet.airobotui.comp.dialogue.UserMessageBubble
import com.airobot.tablet.airobotui.state.RobotVisualState
import com.airobot.services.state.ServiceSubState
import com.airobot.framework.theme.RobotTheme

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
    serviceSubState: ServiceSubState,
    userMessage: String? = null,
    audioLevel: Float = 0.0f,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onTimerControl: (String) -> Unit, // "PAUSE", "RESUME", "STOP"
    onCommandClick: (String) -> Unit = {}, // 新增：点击推荐指令的回调
    modifier: Modifier = Modifier
) {
    val isListening = robotState == RobotVisualState.LISTENING
    val isThinking = robotState == RobotVisualState.THINKING
    val isSpeaking = robotState == RobotVisualState.SPEAKING
    val isTimerActive = serviceSubState != ServiceSubState.IDLE
    
    ConstraintLayout(
        modifier = modifier
    ) {
        val (contentRef, bubbleRef) = createRefs()

        Box(
            modifier = Modifier.constrainAs(contentRef) {
                centerTo(parent)
            }
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
                        audioLevel = audioLevel,
                        onStartListening = onStartListening
                    )
                    "TIMER" -> TimerControlPanel(
                        serviceSubState = serviceSubState,
                        onTimerControl = onTimerControl
                    )
                    "ACTIVE" -> ActiveStatusPanel(
                        isListening = isListening,
                        isThinking = isThinking,
                        isSpeaking = isSpeaking,
                        audioLevel = audioLevel,
                        onStopListening = onStopListening,
                        onCommandClick = onCommandClick
                    )
                }
            }
        }

        // 1. 用户气泡放在左侧，与面板中心水平对齐(只在对话状态显示)
        if(!userMessage.isNullOrBlank() && (robotState == RobotVisualState.LISTENING
                                || robotState == RobotVisualState.THINKING
                                || robotState == RobotVisualState.SPEAKING)) {
            Box(
                modifier = Modifier.constrainAs(bubbleRef) {
                    end.linkTo(contentRef.start, margin = 40.dp)
                    centerVerticallyTo(contentRef)
                }
            ) {
                UserMessageBubble(
                    message = userMessage
                )
            }
        }
    }
}

/**
 * 空闲状态麦克风按钮 (Waiting State)
 * 支持点击唤醒和声浪反馈
 */
@Composable
private fun IdleMicButton(
    isConnected: Boolean,
    audioLevel: Float,
    onStartListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 基础呼吸动画
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val baseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f, // 略微增加呼吸感
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "baseScale"
    )
    
    // 动态声浪缩放 (叠加在呼吸之上)
    // audioLevel (0~1) -> extraScale (0~0.8)
    val dynamicScale by animateFloatAsState(
        targetValue = 1.0f + (audioLevel * 0.4f), // 减小声浪灵敏度，更稳重
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "audioScale"
    )
    
    val finalScale = if (audioLevel > 0.05f) dynamicScale else baseScale

    val ringAlpha by animateFloatAsState(
        targetValue = if (audioLevel > 0.05f) 0.6f + (audioLevel * 0.4f) else 0.2f,
        label = "ringAlpha"
    )
    
    Column(
        modifier = modifier.height(180.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically) // 增加间距 16 -> 24，防止重叠
    ) {
        // 麦克风按钮
        Box(
            modifier = Modifier
                .size(110.dp) // 140 -> 110
                .clickable(enabled = isConnected) { onStartListening() }, // 点击唤醒
            contentAlignment = Alignment.Center
        ) {
            // 动态响应环 - 分层光圈，移除突兀的蓝色实线圈
            if (isConnected) {
                // 底层大光晕 (130 -> 120)
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(finalScale)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF818CF8).copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                        .blur(16.dp)
                )
                // 中层核心光圈 (100 -> 92)
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF818CF8).copy(alpha = 0.35f),
                                    Color(0xFF3B82F6).copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                        .blur(4.dp)
                )
                // 新增：深蓝色声音感应色块圈 (Donut-style band) - 提高亮度与对比度
                Box(
                    modifier = Modifier
                        .size(105.dp) // 尺寸介于核心光圈和按钮之间
                        .scale(finalScale)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                radius = 200f, // 增大渐变范围
                                colors = listOf(
                                    Color(0xFF3B82F6).copy(alpha = 0.6f), // Blue-500 亮蓝色
                                    Color(0xFF2563EB).copy(alpha = 0.3f), // Blue-600
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            
            // 按钮主体 (76 -> 70, 减小约 8%)
            Box(
                modifier = Modifier
                    .size(70.dp) 
                    .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isConnected) {
                                if (RobotTheme.isDark) {
                                    listOf(Color(0xFF334155), Color(0xFF1E293B))
                                } else {
                                    listOf(Color.White, Color(0xFFF1F5F9))
                                }
                            } else {
                                listOf(RobotTheme.colors.surfaceOverlay.copy(0.1f), RobotTheme.colors.surfaceOverlay.copy(0.05f))
                            }
                        )
                    )
                    .clickable(enabled = isConnected)
                    { if (isConnected) onStartListening() }
                    ,
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.mic),
                    contentDescription = "点击唤醒",
                    modifier = Modifier.size(28.dp),
                    tint = if (isConnected) {
                        if (RobotTheme.isDark) Color(0xFF818CF8) else Color(0xFFF97316)
                    } else {
                        RobotTheme.colors.textMuted.copy(alpha = 0.4f)
                    }
                )
            }
        }
        
        // 提示文字 - 增加透明胶囊感
        VoiceHintText(
            text = if (isConnected) "叫名字，开始对话" else "等待连接..."
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
            .clip(RoundedCornerShape(32.dp))
            .background(RobotTheme.colors.cardBg.copy(alpha = 0.95f)) 
            .border(1.dp, RobotTheme.colors.surfaceOverlay.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.chat),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = RobotTheme.colors.accent
        )
        Text(
            text = text,
            color = RobotTheme.colors.textPrimary, 
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
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
    onCommandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "activeStatus")
    
    Column(
        modifier = modifier.height(180.dp), // 同步高度
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(RobotTheme.colors.cardBg.copy(alpha = 0.8f)) // 修改为深蓝色胶囊，匹配原型
                .padding(horizontal = 32.dp, vertical = 18.dp)
                .clickable(enabled = isListening) { onStopListening() },
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                isListening -> {
                        VoiceWaveform(
                            isActive = true,
                            barColor = RobotTheme.colors.accent, // 紫色波形
                            audioLevel = audioLevel
                        )
                        Text(
                            text = "请说话...",
                            color = RobotTheme.colors.textPrimary.copy(alpha = 0.8f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
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
                            .size(22.dp)
                            .graphicsLayer { rotationZ = rotation },
                        tint = RobotTheme.colors.accent
                    )
                    Text(
                        text = "思考中",
                        color = RobotTheme.colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
                isSpeaking -> {
                    SpeakingDots(
                        dotColor = RobotTheme.colors.accent
                    )
                    Text(
                        text = "正在播放回复", // 更匹配原型的状态描述
                        color = RobotTheme.colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        }

        // 快捷建议指令 - 紧随胶囊下方
        QuickCommandChips(
            commands = listOf("打开知识问答", "打开互助播报", "讲个笑话吧"),
            onCommandClick = onCommandClick
        )
    }
}

/**
 * 快捷命令芯片组
 */
@Composable
private fun QuickCommandChips(
    commands: List<String>,
    onCommandClick: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        commands.forEach { text ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(RobotTheme.colors.cardBg.copy(alpha = 0.8f))
                    .clickable { onCommandClick(text) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = text, 
                    color = RobotTheme.colors.textPrimary.copy(alpha = 0.8f), 
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 计时器控制面板
 */
@Composable
private fun TimerControlPanel(
    serviceSubState: ServiceSubState,
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
                    if (serviceSubState == ServiceSubState.PAUSED) 
                        RobotTheme.colors.surfaceOverlay.copy(alpha = 0.2f) 
                    else 
                        RobotTheme.colors.surfaceOverlay.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (serviceSubState == ServiceSubState.RUNNING) {
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
                tint = if (serviceSubState == ServiceSubState.PAUSED) 
                    RobotTheme.colors.textMuted
                else 
                    RobotTheme.colors.accent
            )
        }
        
        // 控制按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 暂停/继续按钮
            if (serviceSubState == ServiceSubState.RUNNING) {
                TimerControlChip(
                    iconResId = R.drawable.volume_low, // pause icon
                    text = "暂停计时",
                    iconColor = Color(0xFFFACC15), // yellow-400
                    onClick = { onTimerControl("PAUSE") }
                )
            } else if (serviceSubState == ServiceSubState.PAUSED) {
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
            .background(RobotTheme.colors.surfaceOverlay.copy(alpha = 0.05f))
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
            color = RobotTheme.colors.textPrimary.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


