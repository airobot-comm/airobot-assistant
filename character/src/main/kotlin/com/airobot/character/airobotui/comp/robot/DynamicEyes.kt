package com.airobot.character.airobotui.comp.robot

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.airobot.framework.theme.RobotEyeActive
import com.airobot.framework.theme.RobotEyeDefault
import com.airobot.framework.theme.StatusCyan

/**
 * 澧炲己鐨勫姩鎬佺溂鐫涚粍浠?- 鏀寔寰〃鎯呭悓姝?
 *
 * Web鍘熷瀷瀵瑰簲: IPCharacter.tsx 涓殑 getEyes() 鍑芥暟
 */
@Composable
fun DynamicEyes(
    state: com.airobot.character.airobotui.state.RobotVisualState,
    ttsProgressNormalized: Float = 0f, // 0-1, TTS鎾斁杩涘害
    audioLevel: () -> Float = { 0f }, // 浼犲叆闊抽绛夌骇 0-1 (Lambda)
    eyeSize: Dp = 48.dp,
    eyeGap: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "enhancedEyeAnimation")

    // 璇磋瘽鏃剁殑鐪肩潧杞姩鍋忕Щ锛堢紦鎱㈠乏鍙崇Щ鍔級
    val speakingEyeLookX by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speakingEyeLook"
    )

    // 鎬濊€冩椂鐨勭溂鐫涢绉伙紙鏇村ぇ骞呭害鐨勪笂涓嬪乏鍙崇Щ鍔級
    val thinkingEyeOffsetX by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thinkingEyeOffsetX"
    )

    val thinkingEyeOffsetY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thinkingEyeOffsetY"
    )

    // 璁＄畻鐪肩潧鐨勫疄闄呭亸绉?
    val eyeOffsetX = when (state) {
        com.airobot.character.airobotui.state.RobotVisualState.SPEAKING -> speakingEyeLookX.dp
        com.airobot.character.airobotui.state.RobotVisualState.THINKING -> thinkingEyeOffsetX.dp
        else -> 0.dp
    }

    val eyeOffsetY = when (state) {
        com.airobot.character.airobotui.state.RobotVisualState.THINKING -> thinkingEyeOffsetY.dp
        else -> 0.dp
    }

    Row(
        modifier = modifier
            .offset(x = eyeOffsetX, y = eyeOffsetY),
        horizontalArrangement = Arrangement.spacedBy(eyeGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EnhancedDynamicEye(
            state = state,
            size = eyeSize,
            ttsProgressNormalized = ttsProgressNormalized,
            audioLevel = audioLevel
        )
        EnhancedDynamicEye(
            state = state,
            size = eyeSize,
            ttsProgressNormalized = ttsProgressNormalized,
            audioLevel = audioLevel
        )
    }
}

/**
 * 鍗曚釜鐪肩潧缁勪欢 - 澧炲己鐗?(甯︽湁鍙戝厜鏁堟灉)
 */
@Composable
private fun EnhancedDynamicEye(
    state: com.airobot.character.airobotui.state.RobotVisualState,
    size: Dp,
    ttsProgressNormalized: Float = 0f,
    audioLevel: () -> Float = { 0f },
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 澶栭儴鍙戝厜灞?(Bloom Effect) - 鏀逛负闀挎き鍦?
        Box(
            modifier = Modifier
                .size(width = size * 1.5f, height = size * 1.2f)
                .clip(RoundedCornerShape(size))
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            getEyeColor(state).copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
                .blur(size * 0.25f)
        )

        // 鏍稿績鐪肩潧缁勪欢
        when (state) {
            com.airobot.character.airobotui.state.RobotVisualState.IDLE -> IdleEyeEnhanced(size = size)
            com.airobot.character.airobotui.state.RobotVisualState.LISTENING -> ListeningEyeEnhanced(size = size, audioLevel = audioLevel)
            com.airobot.character.airobotui.state.RobotVisualState.THINKING -> ThinkingEyeEnhanced(size = size)
            com.airobot.character.airobotui.state.RobotVisualState.SPEAKING -> SpeakingEyeEnhanced(
                size = size,
                ttsProgressNormalized = ttsProgressNormalized,
                audioLevel = audioLevel
            )
            com.airobot.character.airobotui.state.RobotVisualState.FOCUS -> FocusEyeEnhanced(size = size)
            com.airobot.character.airobotui.state.RobotVisualState.HAPPY -> HappyEyeEnhanced(size = size)
            com.airobot.character.airobotui.state.RobotVisualState.SLEEPING -> SleepingEyeEnhanced(size = size)
        }
    }
}

private fun getEyeColor(state: com.airobot.character.airobotui.state.RobotVisualState): Color {
    return when (state) {
        com.airobot.character.airobotui.state.RobotVisualState.IDLE -> RobotEyeDefault
        com.airobot.character.airobotui.state.RobotVisualState.LISTENING -> StatusCyan
        com.airobot.character.airobotui.state.RobotVisualState.THINKING -> RobotEyeActive // Orange
        com.airobot.character.airobotui.state.RobotVisualState.SPEAKING -> RobotEyeActive // Orange
        com.airobot.character.airobotui.state.RobotVisualState.FOCUS -> Color(0xFF67E8F9)
        com.airobot.character.airobotui.state.RobotVisualState.HAPPY -> Color(0xFF10B981)
        com.airobot.character.airobotui.state.RobotVisualState.SLEEPING -> Color(0xFF94A3B8)
    }
}

/**
 * IDLE 鐘舵€佺溂鐫?- 妞渾褰?+ 楂樺厜
 */
@Composable
private fun IdleEyeEnhanced(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(size)
            .height(size * 0.95f) // 鏇村渾涓€鐐癸紝鍖归厤鍘熷瀷
            .clip(CircleShape)
            .background(RobotEyeDefault),
        contentAlignment = Alignment.Center
    ) {
        // 鍗曚釜鏄庝寒楂樺厜 - 鍖归厤鍘熷瀷
        Box(
            modifier = Modifier
                .size(size * 0.35f)
                .offset(x = (size * 0.15f), y = (-size * 0.15f))
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
        )
    }
}

/**
 * LISTENING 鐘舵€佺溂鐫?- 闊抽鎰熷簲楂樺害
 */
@Composable
private fun ListeningEyeEnhanced(
    size: Dp,
    audioLevel: () -> Float, // 澧炲姞闊抽绛夌骇璋冨埗
    modifier: Modifier = Modifier
) {
    val dynamicHeight = size * (0.4f + audioLevel() * 1.0f)
    Box(
        modifier = modifier
            .width(size * 0.9f)
            .height(dynamicHeight)
            .clip(RoundedCornerShape(50))
            .background(getEyeColor(com.airobot.character.airobotui.state.RobotVisualState.LISTENING).copy(alpha = 0.95f))
            .blur(0.5.dp)
    )
}

/**
 * THINKING 鐘舵€佺溂鐫?- 鏃嬭浆鍔犺浇鐜?
 */
@Composable
private fun ThinkingEyeEnhanced(
    size: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinkingEyeArch")
    
    // 鍛煎惛鎰燂細绮楃粏涓庝綅缃交寰彉鍔?
    val breath by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = (size.toPx() * 0.15f) + (breath * 2.dp.toPx())
            drawArc(
                color = RobotEyeActive,
                startAngle = 180f,
                sweepAngle = 180f, // 鎷卞舰锛堝紑鍙ｅ悜涓嬶級
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * SPEAKING 鐘舵€佺溂鐫?- 缂╂斁鑴夊啿 + 闊抽璋冨埗
 */
@Composable
private fun SpeakingEyeEnhanced(
    size: Dp,
    ttsProgressNormalized: Float = 0f,
    audioLevel: () -> Float = { 0f },
    modifier: Modifier = Modifier
) {
    val audioEffect = audioLevel()
    
    // 璇磋瘽鏃朵篃鏄鑹叉嫳褰紝浣嗕細闅忕潃澹伴煶娉㈠姩鈥滃紶鍚堚€?
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = (size.toPx() * 0.15f) + (audioEffect * 5.dp.toPx())
            // 鎵繃瑙掑害闅忓０闊冲彉鍖栵紝浜х敓鐪ㄥ姩鎰?
            val sweep = 180f - (audioEffect * 20f)
            val start = 180f + (audioEffect * 10f)
            
            drawArc(
                color = RobotEyeActive,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * FOCUS 鐘舵€佺溂鐫?- 鎵佸钩绂呮剰鐪肩潧 (鏋佺獎妞渾)
 */
@Composable
private fun FocusEyeEnhanced(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(size * 1.3f)
            .height(size * 0.25f)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF67E8F9).copy(alpha = 0.85f)) // cyan-300
    )
}

/**
 * HAPPY 鐘舵€佺溂鐫?- 寮集绗戠溂
 */
@Composable
private fun HappyEyeEnhanced(
    size: Dp,
    modifier: Modifier = Modifier
) {
    // 渚濈劧浣跨敤妞渾浣滀负鍩虹
    Box(
        modifier = modifier
            .width(size)
            .height(size * 0.8f)
            .clip(RoundedCornerShape(50))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF10B981), // green-500
                        Color(0xFF059669)  // green-600
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // 绗戠溂鐨勫姬褰㈤伄鎸?(绠€鍗曞疄鐜帮細閫氳繃涓婃柟棰滆壊瑕嗙洊)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (size * 0.35f))
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.8f))
        )
    }
}

/**
 * SLEEPING 鐘舵€佺溂鐫?- 闂溂
 */
@Composable
private fun SleepingEyeEnhanced(
    size: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sleepingEye")

    // 缂撴參鍛煎惛鍔ㄧ敾
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sleepingBreath"
    )

    Box(
        modifier = modifier
            .width(size * 1.1f * breathScale)
            .height(size * 0.08f)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF94A3B8).copy(alpha = 0.6f)) // slate-400
    )
}


