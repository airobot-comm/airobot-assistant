package com.airobot.airbot.character.canvas.aether

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.airobot.airbot.viewmodel.RobotVisualState
import com.airobot.framework.theme.AetherEarInner

// ============================================================================
// AetherEarShape — Custom pointed yet soft and rounded tapered shape for cat ears.
// Tapers from a wider base (utilizing full box width) to a soft, rounded tip.
// ============================================================================
private class AetherEarShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            // Start at bottom left
            moveTo(0f, size.height)
            // Outer curved edge to a beautifully rounded, cute top tip
            cubicTo(
                x1 = size.width * 0.05f, y1 = size.height * 0.45f,
                x2 = size.width * 0.18f, y2 = size.height * 0.15f,
                x3 = size.width * 0.5f, y3 = size.height * 0.04f // Rounded tip, slightly down
            )
            // Inner curved edge from tip to bottom right
            cubicTo(
                x1 = size.width * 0.82f, y1 = size.height * 0.15f,
                x2 = size.width * 0.95f, y2 = size.height * 0.45f,
                x3 = size.width, y3 = size.height
            )
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Independent ear component for the Aether robot character.
 *
 * @param state Current visual state — drives ear droop and swing behavior
 * @param earSwing Ear swing amplitude in degrees (from BodyAnimParams)
 * @param earDroop Ear base droop offset in degrees (from BodyAnimParams)
 * @param shellSize The outer shell's diameter — used for positioning
 * @param modifier Parent modifier
 */
@Composable
internal fun DynamicEars(
    state: RobotVisualState,
    earSwing: Float,
    earDroop: Float,
    shellSize: Dp,
    modifier: Modifier = Modifier
) {
    // Smooth transition for ear swing and droop targets
    val earSwingTarget by animateFloatAsState(
        targetValue = earSwing,
        animationSpec = tween(600),
        label = "earSwingTarget"
    )
    val earDroopTarget by animateFloatAsState(
        targetValue = earDroop,
        animationSpec = tween(800),
        label = "earDroopTarget"
    )

    // Continuous ear animation — fast oscillation during speaking, slow idle sway
    val infiniteTransition = rememberInfiniteTransition(label = "earBounce")
    val talkingEarBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == RobotVisualState.SPEAKING) 800 else 4000,
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "earBounceY"
    )

    // Ear swing oscillation
    val earSwingAnim by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    RobotVisualState.SPEAKING -> 800
                    RobotVisualState.LISTENING -> 1500
                    else -> 4000
                },
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "earSwingOsc"
    )

    // Dimensions — wider, softer, organic cat-ear shape
    val earWidth = shellSize * 0.30f  // 3) Wider shape (was 0.26f)
    val earHeight = shellSize * 0.42f

    val earShape = remember { AetherEarShape() }

    // Gradient: from white (tip) to warm rose-white (base) — matches inner face plate
    val earGradient = Brush.verticalGradient(
        listOf(Color.White, AetherEarInner)
    )

    // Y offset: positioned lower (closer to the face plate, base overlapping shell).
    // The face plate is drawn on top of the ears, which naturally covers their bases,
    // making the exposed tip part shorter.
    val earBaseY = -shellSize * 0.35f + talkingEarBounce.dp

    // Current animated swing angle
    val currentSwing = earSwingTarget * earSwingAnim

    // Left ear
    Box(
        modifier = modifier
            .offset(
                x = -shellSize * 0.22f,
                y = earBaseY
            )
            .size(width = earWidth, height = earHeight)
            .graphicsLayer {
                rotationZ = earDroopTarget + currentSwing - 12f  // Base -12° outward tilt
                transformOrigin = TransformOrigin(0.5f, 0.95f)   // Pivot at base
                alpha = 0.98f                                    // 1) Opaque / low transparency
            }
            .shadow(2.dp, earShape)
            .clip(earShape)
            .background(earGradient)
            .border(4.dp, Color.White, earShape)              // 2) Thicker white border frame
    )

    // Right ear
    Box(
        modifier = modifier
            .offset(
                x = shellSize * 0.22f,
                y = earBaseY
            )
            .size(width = earWidth, height = earHeight)
            .graphicsLayer {
                rotationZ = -(earDroopTarget + currentSwing) + 12f  // Base +12° outward tilt
                transformOrigin = TransformOrigin(0.5f, 0.95f)
                alpha = 0.98f                                    // 1) Opaque / low transparency
            }
            .shadow(2.dp, earShape)
            .clip(earShape)
            .background(earGradient)
            .border(4.dp, Color.White, earShape)              // 2) Thicker white border frame
    )
}
