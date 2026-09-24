package com.airobot.airbot.character.canvas.aether

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.airbot.character.canvas.CanvasCharacterProfile
import com.airobot.airbot.viewmodel.RobotVisualState
import com.airobot.framework.theme.AetherHandBorder
import com.airobot.framework.theme.AetherMouthReady
import com.airobot.framework.theme.AetherMouthTalking
import com.airobot.framework.theme.AetherShadowColor
import com.airobot.framework.theme.AetherShellEnd
import com.airobot.framework.theme.AetherShellStart
import com.airobot.framework.theme.AetherShellVia
import com.airobot.framework.theme.RobotTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

// ============================================================================
// Animation parameter data class — centralizes per-state motion config
// ============================================================================

private data class BodyAnimParams(
    val floatRange: Float,
    val floatDuration: Int,
    val floatXRange: Float = 0f,
    val floatXDuration: Int = 4000,
    val rotateRange: Float,
    val rotateDuration: Int,
    val earSwing: Float,
    val earDroop: Float = 0f,
    val showBlush: Boolean = false
)

private fun animParamsFor(state: RobotVisualState): BodyAnimParams = when (state) {
    RobotVisualState.IDLE -> BodyAnimParams(
        floatRange = 15f, floatDuration = 3500,
        rotateRange = 3f, rotateDuration = 3500,
        earSwing = 10f, showBlush = true
    )

    RobotVisualState.BORED -> BodyAnimParams(
        floatRange = 8f, floatDuration = 4500,
        floatXRange = 20f, floatXDuration = 4500,
        rotateRange = 12f, rotateDuration = 4500,
        earSwing = 4f, earDroop = -20f
    )

    RobotVisualState.DAZING -> BodyAnimParams(
        floatRange = 35f, floatDuration = 8000,
        floatXRange = 45f, floatXDuration = 8000,
        rotateRange = 15f, rotateDuration = 8000,
        earSwing = 6f, showBlush = true
    )

    RobotVisualState.DOZING -> BodyAnimParams(
        floatRange = -10f, floatDuration = 3500,
        rotateRange = 10f, rotateDuration = 3500,
        earSwing = 3f, earDroop = -15f
    )

    RobotVisualState.SLEEPING -> BodyAnimParams(
        floatRange = -15f, floatDuration = 4500,
        rotateRange = 2f, rotateDuration = 4500,
        earSwing = 0f, earDroop = -40f
    )

    RobotVisualState.LISTENING -> BodyAnimParams(
        floatRange = 10f, floatDuration = 1500,
        rotateRange = 3f, rotateDuration = 1500,
        earSwing = 10f, showBlush = true
    )

    RobotVisualState.THINKING -> BodyAnimParams(
        floatRange = 6f, floatDuration = 600,
        rotateRange = 2f, rotateDuration = 600,
        earSwing = 10f
    )

    RobotVisualState.SPEAKING -> BodyAnimParams(
        floatRange = 18f, floatDuration = 1200,
        rotateRange = 6f, rotateDuration = 1200,
        earSwing = 15f, showBlush = true
    )

    RobotVisualState.FOCUS -> BodyAnimParams(
        floatRange = 4f, floatDuration = 3000,
        rotateRange = 1f, rotateDuration = 3000,
        earSwing = 5f
    )

    RobotVisualState.HAPPY -> BodyAnimParams(
        floatRange = 12f, floatDuration = 1200,
        rotateRange = 4f, rotateDuration = 1200,
        earSwing = 10f, showBlush = true
    )

    RobotVisualState.WORKING -> BodyAnimParams(
        floatRange = 6f, floatDuration = 600,
        rotateRange = 2f, rotateDuration = 600,
        earSwing = 5f
    )
}

/**
 * Aether Profile - The default skeuomorphic animated character.
 */
object AetherProfileCanvas : CanvasCharacterProfile {
    @Composable
    override fun Render(
        state: RobotVisualState,
        ttsProgressNormalized: Float,
        audioLevel: () -> Float,
        size: Dp,
        showAura: Boolean,
        modifier: Modifier
    ) {
        val params = animParamsFor(state)
        val infiniteTransition = rememberInfiniteTransition(label = "robotAetherAnimation")
        val isBlinking = rememberBlinkState()

        val floatY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -params.floatRange,
            animationSpec = infiniteRepeatable(
                animation = tween(params.floatDuration, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "floatY"
        )

        val floatX by infiniteTransition.animateFloat(
            initialValue = -params.floatXRange,
            targetValue = params.floatXRange,
            animationSpec = infiniteRepeatable(
                animation = tween(params.floatXDuration, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "floatX"
        )

        val bodyRotate by infiniteTransition.animateFloat(
            initialValue = -params.rotateRange,
            targetValue = params.rotateRange,
            animationSpec = infiniteRepeatable(
                animation = tween(params.rotateDuration, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bodyRotate"
        )

        Box(
            modifier = modifier
                .offset(x = floatX.dp, y = floatY.dp)
                .graphicsLayer { rotationZ = bodyRotate },
            contentAlignment = Alignment.Center
        ) {
            if (showAura) {
                Box(
                    modifier = Modifier
                        .size(size * 1.2f)
                        .blur(50.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    RobotTheme.colors.robotAuraStart,
                                    RobotTheme.colors.robotAuraEnd,
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
            }

            RobotAetherBody(
                state = state,
                params = params,
                isBlinking = isBlinking,
                audioLevel = audioLevel,
                ttsProgressNormalized = ttsProgressNormalized,
                size = size
            )
        }
    }
}

@Composable
private fun RobotAetherBody(
    state: RobotVisualState,
    params: BodyAnimParams,
    isBlinking: Boolean,
    audioLevel: () -> Float,
    ttsProgressNormalized: Float,
    size: Dp
) {
    val shellSize = size * 0.88f
    val shellCorner = size * 0.38f

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        val shellColors = if (state == RobotVisualState.WORKING) {
            listOf(Color(0xFFDBEAFE), Color(0xFFCFFAFE), Color(0xFFE0E7FF))
        } else {
            listOf(AetherShellStart, AetherShellVia, AetherShellEnd)
        }
        val shellShadowColor = if (state == RobotVisualState.WORKING) {
            Color(0xFF38BDF8).copy(alpha = 0.5f)
        } else {
            AetherShadowColor
        }

        Box(
            modifier = Modifier
                .size(shellSize)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(shellCorner),
                    spotColor = shellShadowColor
                )
                .clip(RoundedCornerShape(shellCorner))
                .background(
                    brush = Brush.linearGradient(shellColors)
                )
                .border(4.dp, Color.White, RoundedCornerShape(shellCorner))
        )

        DynamicEars(
            state = state,
            earSwing = params.earSwing,
            earDroop = params.earDroop,
            shellSize = shellSize
        )

        val faceSize = shellSize * 0.82f
        val faceColors = if (state == RobotVisualState.WORKING) {
            listOf(Color.White, Color(0xFFEFF6FF))
        } else {
            listOf(
                Color.White,
                Color(0xFFFFFBF5),
                Color(0xFFFFF7ED)
            )
        }
        Box(
            modifier = Modifier
                .size(faceSize)
                .clip(RoundedCornerShape(size * 0.32f))
                .background(
                    brush = Brush.linearGradient(faceColors)
                )
                .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(size * 0.32f))
        ) {
            AetherBlush(
                showBlush = params.showBlush,
                size = size
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                DynamicEyes(
                    state = state,
                    ttsProgressNormalized = ttsProgressNormalized,
                    audioLevel = audioLevel,
                    eyeSize = size * 0.18f,
                    eyeGap = size * 0.14f
                )

                Spacer(modifier = Modifier.height(size * 0.08f))

                AetherMouth(state = state, audioLevel = audioLevel, size = size)
            }

            if (state == RobotVisualState.WORKING) {
                val infiniteTransition = rememberInfiniteTransition(label = "laserScan")
                val laserY by infiniteTransition.animateFloat(
                    initialValue = -30f,
                    targetValue = 30f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "laserY"
                )
                val laserAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 0.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "laserAlpha"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.Center)
                        .offset(y = laserY.dp)
                        .background(Color(0xFF22D3EE).copy(alpha = laserAlpha))
                        .blur(2.dp)
                        .shadow(10.dp, spotColor = Color(0xFF22D3EE))
                )
            }
        }

        AetherHands(state = state, size = size)

        if (state == RobotVisualState.SLEEPING) {
            SleepingZzzParticles(size = size)
        }
    }
}

@Composable
private fun SleepingZzzParticles(size: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "zzzParticles")

    val z1Y by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = -40f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Restart
        ),
        label = "z1Y"
    )
    val z1Alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2500
                0f at 0
                1f at 800
                1f at 1600
                0f at 2500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "z1Alpha"
    )
    val z1X by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2500
                0f at 0
                10f at 1250
                0f at 2500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "z1X"
    )

    val z2Y by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = -50f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, delayMillis = 1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Restart
        ),
        label = "z2Y"
    )
    val z2Alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                0f at 0
                0f at 1000
                1f at 1800
                1f at 2800
                0f at 4000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "z2Alpha"
    )
    val z2X by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                0f at 0
                0f at 1000
                -10f at 2500
                0f at 4000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "z2X"
    )

    Text(
        text = "z",
        color = Color(0xFFFDBA74).copy(alpha = z1Alpha),
        fontWeight = FontWeight.Black,
        fontSize = 18.sp,
        modifier = Modifier
            .offset(x = size * 0.28f + z1X.dp, y = -size * 0.35f + z1Y.dp)
    )

    Text(
        text = "Z",
        color = Color(0xFFFB923C).copy(alpha = z2Alpha),
        fontWeight = FontWeight.Black,
        fontSize = 22.sp,
        modifier = Modifier
            .offset(x = size * 0.32f + z2X.dp, y = -size * 0.45f + z2Y.dp)
    )
}

@Composable
private fun AetherBlush(showBlush: Boolean, size: Dp) {
    val blushAlpha by animateFloatAsState(
        targetValue = if (showBlush) 1f else 0f,
        animationSpec = tween(500),
        label = "blushFade"
    )

    if (blushAlpha > 0.01f) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val blushRadius = size.toPx() * 0.08f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE11D48).copy(alpha = 0.55f * blushAlpha),
                        Color(0xFFF472B6).copy(alpha = 0.35f * blushAlpha),
                        Color.Transparent
                    ),
                    center = center.copy(x = size.toPx() * 0.26f, y = size.toPx() * 0.46f),
                    radius = blushRadius
                ),
                radius = blushRadius,
                center = center.copy(x = size.toPx() * 0.26f, y = size.toPx() * 0.46f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE11D48).copy(alpha = 0.55f * blushAlpha),
                        Color(0xFFF472B6).copy(alpha = 0.35f * blushAlpha),
                        Color.Transparent
                    ),
                    center = center.copy(x = size.toPx() * 0.56f, y = size.toPx() * 0.46f),
                    radius = blushRadius
                ),
                radius = blushRadius,
                center = center.copy(x = size.toPx() * 0.56f, y = size.toPx() * 0.46f)
            )
        }
    }
}

@Composable
private fun AetherMouth(state: RobotVisualState, audioLevel: () -> Float, size: Dp) {
    when {
        state == RobotVisualState.SPEAKING -> {
            val mouthHeight by animateDpAsState(
                targetValue = (3.dp + (audioLevel() * 15).dp),
                label = "mouthH"
            )
            val mouthWidth by animateDpAsState(
                targetValue = (12.dp + (audioLevel() * 8).dp),
                label = "mouthW"
            )
            Box(
                modifier = Modifier
                    .size(width = mouthWidth, height = mouthHeight)
                    .clip(CircleShape)
                    .background(AetherMouthTalking)
            )
        }

        state == RobotVisualState.SLEEPING || state == RobotVisualState.DOZING -> {
            Box(
                modifier = Modifier
                    .size(width = size * 0.06f, height = size * 0.03f)
                    .clip(
                        RoundedCornerShape(
                            topStartPercent = 50, topEndPercent = 50,
                            bottomStartPercent = 10, bottomEndPercent = 10
                        )
                    )
                    .background(Color(0xFF334155).copy(alpha = 0.7f))
            )
        }

        else -> {
            Box(
                modifier = Modifier
                    .size(width = size * 0.08f, height = 2.5.dp)
                    .clip(CircleShape)
                    .background(AetherMouthReady)
            )
        }
    }
}

@Composable
private fun AetherHands(state: RobotVisualState, size: Dp) {
    val handSize = size * 0.18f

    val handFloat by rememberInfiniteTransition(label = "handAnim").animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "handFloat"
    )

    val handDroop = when (state) {
        RobotVisualState.SLEEPING -> 15f
        RobotVisualState.DOZING -> 10f
        else -> 0f
    }
    val animatedDroop by animateFloatAsState(
        targetValue = handDroop,
        animationSpec = tween(600),
        label = "handDroop"
    )

    val handRotation = when (state) {
        RobotVisualState.SLEEPING -> -15f
        RobotVisualState.DAZING -> 0f
        else -> 0f
    }
    val animatedRotation by animateFloatAsState(
        targetValue = handRotation,
        animationSpec = tween(600),
        label = "handRotation"
    )

    val dazingHandSwing by rememberInfiniteTransition(label = "dazingHand").animateFloat(
        initialValue = -40f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dazingSwing"
    )

    val leftHandRotation = when (state) {
        RobotVisualState.SLEEPING -> animatedRotation
        RobotVisualState.DAZING -> dazingHandSwing
        RobotVisualState.WORKING -> dazingHandSwing * 0.5f
        else -> animatedRotation
    }
    val rightHandRotation = when (state) {
        RobotVisualState.SLEEPING -> -animatedRotation
        RobotVisualState.DAZING -> -dazingHandSwing
        RobotVisualState.WORKING -> -dazingHandSwing * 0.5f
        else -> -animatedRotation
    }

    val sleepingHandX = when (state) {
        RobotVisualState.SLEEPING -> -10f
        else -> 0f
    }
    val animatedSleepX by animateFloatAsState(
        targetValue = sleepingHandX,
        animationSpec = tween(600),
        label = "handSleepX"
    )

    Box(
        modifier = Modifier
            .offset(
                x = -size * 0.40f + animatedSleepX.dp,
                y = size * 0.10f + handFloat.dp + animatedDroop.dp
            )
            .size(handSize)
            .graphicsLayer { rotationZ = leftHandRotation }
            .shadow(6.dp, CircleShape)
            .background(Color.White, CircleShape)
            .border(2.dp, AetherHandBorder, CircleShape)
    )

    Box(
        modifier = Modifier
            .offset(
                x = size * 0.40f + (-animatedSleepX).dp,
                y = size * 0.10f + (handFloat * 0.7f).dp + animatedDroop.dp
            )
            .size(handSize)
            .graphicsLayer { rotationZ = rightHandRotation }
            .shadow(6.dp, CircleShape)
            .background(Color.White, CircleShape)
            .border(2.dp, AetherHandBorder, CircleShape)
    )
}

@Composable
private fun rememberBlinkState(): Boolean {
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2000, 5000))
            isBlinking = true
            delay(120)
            isBlinking = false
        }
    }
    return isBlinking
}
