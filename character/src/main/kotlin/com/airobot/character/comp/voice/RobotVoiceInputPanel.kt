package com.airobot.character.comp.voice

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
import com.airobot.character.comp.dialogue.UserMessageBubble
import com.airobot.character.state.RobotVisualState
import com.airobot.services.state.ServiceSubState
import com.airobot.framework.theme.RobotTheme

/**
 * 鏈哄櫒浜洪鏍艰闊宠緭鍏ラ潰鏉?
 * 
 * Web鍘熷瀷瀵瑰簲: VoiceInputPanel.tsx
 * 
 * 鍔熻兘:
 * - 绌洪棽鐘舵€侊細楹﹀厠椋庢寜閽?+ 鎻愮ず鏂囧瓧
 * - 鑱嗗惉鐘舵€侊細娉㈠舰鍔ㄧ敾 + 鐘舵€佹彁绀?
 * - 鎬濊€冪姸鎬侊細鍔犺浇鍔ㄧ敾
 * - 璇磋瘽鐘舵€侊細鎾斁鎸囩ず
 * - 涓撴敞妯″紡锛氳鏃跺櫒鎺у埗鎸夐挳
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
    onCommandClick: (String) -> Unit = {}, // 鏂板锛氱偣鍑绘帹鑽愭寚浠ょ殑鍥炶皟
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

        // 1. 鐢ㄦ埛姘旀场鏀惧湪宸︿晶锛屼笌闈㈡澘涓績姘村钩瀵归綈(鍙湪瀵硅瘽鐘舵€佹樉绀?
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
 * 绌洪棽鐘舵€侀害鍏嬮鎸夐挳 (Waiting State)
 * 鏀寔鐐瑰嚮鍞ら啋鍜屽０娴弽棣?
 */
@Composable
private fun IdleMicButton(
    isConnected: Boolean,
    audioLevel: Float,
    onStartListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 鍩虹鍛煎惛鍔ㄧ敾
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val baseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f, // 鐣ュ井澧炲姞鍛煎惛鎰?
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "baseScale"
    )
    
    // 鍔ㄦ€佸０娴缉鏀?(鍙犲姞鍦ㄥ懠鍚镐箣涓?
    // audioLevel (0~1) -> extraScale (0~0.8)
    val dynamicScale by animateFloatAsState(
        targetValue = 1.0f + (audioLevel * 0.4f), // 鍑忓皬澹版氮鐏垫晱搴︼紝鏇寸ǔ閲?
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
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically) // 澧炲姞闂磋窛 16 -> 24锛岄槻姝㈤噸鍙?
    ) {
        // 楹﹀厠椋庢寜閽?
        Box(
            modifier = Modifier
                .size(110.dp) // 140 -> 110
                .clickable(enabled = isConnected) { onStartListening() }, // 鐐瑰嚮鍞ら啋
            contentAlignment = Alignment.Center
        ) {
            // 鍔ㄦ€佸搷搴旂幆 - 鍒嗗眰鍏夊湀锛岀Щ闄ょ獊鍏€鐨勮摑鑹插疄绾垮湀
            if (isConnected) {
                // 搴曞眰澶у厜鏅?(130 -> 120)
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
                // 涓眰鏍稿績鍏夊湀 (100 -> 92)
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
                // 鏂板锛氭繁钃濊壊澹伴煶鎰熷簲鑹插潡鍦?(Donut-style band) - 鎻愰珮浜害涓庡姣斿害
                Box(
                    modifier = Modifier
                        .size(105.dp) // 灏哄浠嬩簬鏍稿績鍏夊湀鍜屾寜閽箣闂?
                        .scale(finalScale)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                radius = 200f, // 澧炲ぇ娓愬彉鑼冨洿
                                colors = listOf(
                                    Color(0xFF3B82F6).copy(alpha = 0.6f), // Blue-500 浜摑鑹?
                                    Color(0xFF2563EB).copy(alpha = 0.3f), // Blue-600
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            
            // 鎸夐挳涓讳綋 (76 -> 70, 鍑忓皬绾?8%)
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
                    contentDescription = "鐐瑰嚮鍞ら啋",
                    modifier = Modifier.size(28.dp),
                    tint = if (isConnected) {
                        if (RobotTheme.isDark) Color(0xFF818CF8) else Color(0xFFF97316)
                    } else {
                        RobotTheme.colors.textMuted.copy(alpha = 0.4f)
                    }
                )
            }
        }
        
        // 鎻愮ず鏂囧瓧 - 澧炲姞閫忔槑鑳跺泭鎰?
        VoiceHintText(
            text = if (isConnected) "叫名字，开始对话" else "等待连接..."
        )
    }
}

/**
 * 缁熶竴鐨勮闊虫彁绀烘枃瀛楅€昏緫
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
 * 娲昏穬鐘舵€侀潰鏉?
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
        modifier = modifier.height(180.dp), // 鍚屾楂樺害
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(RobotTheme.colors.cardBg.copy(alpha = 0.8f)) // 淇敼涓烘繁钃濊壊鑳跺泭锛屽尮閰嶅師鍨?
                .padding(horizontal = 32.dp, vertical = 18.dp)
                .clickable(enabled = isListening) { onStopListening() },
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                isListening -> {
                        VoiceWaveform(
                            isActive = true,
                            barColor = RobotTheme.colors.accent, // 绱壊娉㈠舰
                            audioLevel = audioLevel
                        )
                        Text(
                            text = "璇疯璇?..",
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
                        text = "鎬濊€冧腑",
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
                        text = "姝ｅ湪鎾斁鍥炲", // 鏇村尮閰嶅師鍨嬬殑鐘舵€佹弿杩?
                        color = RobotTheme.colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        }

        // 蹇嵎寤鸿鎸囦护 - 绱ч殢鑳跺泭涓嬫柟
        QuickCommandChips(
            commands = listOf("打开知识问答", "打开互助播报", "讲个笑话吧"),
            onCommandClick = onCommandClick
        )
    }
}

/**
 * 蹇嵎鍛戒护鑺墖缁?
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
 * 璁℃椂鍣ㄦ帶鍒堕潰鏉?
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
        // 娌欐紡鍥炬爣
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
                // 鏃嬭浆杈规
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
        
        // 鎺у埗鎸夐挳
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 鏆傚仠/缁х画鎸夐挳
            if (serviceSubState == ServiceSubState.RUNNING) {
                TimerControlChip(
                    iconResId = R.drawable.volume_low, // pause icon
                    text = "鏆傚仠璁℃椂",
                    iconColor = Color(0xFFFACC15), // yellow-400
                    onClick = { onTimerControl("PAUSE") }
                )
            } else if (serviceSubState == ServiceSubState.PAUSED) {
                TimerControlChip(
                    iconResId = R.drawable.mic, // play icon
                    text = "缁х画璁℃椂",
                    iconColor = Color(0xFF34D399), // emerald-400
                    onClick = { onTimerControl("RESUME") }
                )
            }
            
            // 鍋滄鎸夐挳
            TimerControlChip(
                iconResId = R.drawable.close, // stop icon
                text = "缁撴潫涓撴敞",
                iconColor = Color(0xFFF87171), // red-400
                isDestructive = true,
                onClick = { onTimerControl("STOP") }
            )
        }
    }
}

/**
 * 璁℃椂鍣ㄦ帶鍒舵寜閽?
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


