package com.airobot.framework.dial

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Configuration for an icon label around the dial.
 */
data class DialCategoryConfig(
    val label: String,
    val icon: ImageVector,
    val index: Int
)

/**
 * Skeuomorphic Dial — 3-layer rotatable module switching knob.
 *
 * Prototype ref: SkeuomorphicDial.tsx
 *
 * Fixed: Replaced Box+Modifiers with Canvas drawing to eliminate "polygonal" artifacts
 * and ensure perfect circularity and smooth gradients.
 */
@Composable
fun SkeuomorphicDial(
    categories: List<DialCategoryConfig>,
    activeCategoryIndex: Int,
    subCategoryIndex: Int,
    subCategoryCount: Int,
    onCategoryChange: (Int) -> Unit,
    onCenterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = RobotTheme.isDark

    // Animations
    val angleStep = if (categories.isNotEmpty()) 360f / categories.size else 120f
    val targetRotation = activeCategoryIndex * angleStep
    val knobRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "knobRotation"
    )

    val subAngleStep = if (subCategoryCount > 0) 360f / subCategoryCount else 0f
    val subRotation by animateFloatAsState(
        targetValue = subCategoryIndex * subAngleStep,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "subRotation"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        val scaleRatio = (maxWidth / 240.dp).coerceIn(0.5f, 1.1f)
        val trackSize = 171.dp * scaleRatio
        val baseSize = 117.dp * scaleRatio
        val knobSize = 90.dp * scaleRatio
        val centerSize = 48.dp * scaleRatio
        val iconRadius = 85.0 * scaleRatio.toDouble()

        // Layer 1 & 2: Background Track and Base Well (Drawn on Canvas for smoothness)
        Canvas(modifier = Modifier.size(trackSize)) {
            val center = this.center
            val trackRadiusPx = (trackSize / 2).toPx()
            val baseRadiusPx = (baseSize / 2).toPx()

            // 1. Draw Outer Glass Track (Layer 1)
            // Use radial gradient to simulate "beveled glass" + "frosted" look
            drawCircle(
                brush = Brush.radialGradient(
                    0.7f to (if (isDark) DialTrackDark.copy(alpha = 0.15f) else DialTrackLight.copy(alpha = 0.25f)),
                    0.95f to (if (isDark) DialTrackDark.copy(alpha = 0.45f) else DialTrackLight.copy(alpha = 0.55f)),
                    1.0f to (if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)),
                    center = center,
                    radius = trackRadiusPx
                ),
                radius = trackRadiusPx,
                center = center
            )

            // Subtle outer border for Layer 1 clarity
            drawCircle(
                color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f),
                radius = trackRadiusPx,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )

            // 2. Draw Dial Base Well (Layer 2)
            // Simulate "sunken" depth using inner-shadow-like radial gradient
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to (if (isDark) DialBaseDark else DialBaseLight.copy(alpha = 0.4f)),
                    0.85f to (if (isDark) DialBaseDark else DialBaseLight),
                    1.0f to (if (isDark) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.2f)),
                    center = center,
                    radius = baseRadiusPx
                ),
                radius = baseRadiusPx,
                center = center
            )

            // Inner edge shadow for Layer 2
            drawCircle(
                brush = Brush.radialGradient(
                    0.8f to Color.Transparent,
                    1.0f to (if (isDark) Color.Black.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.15f)),
                    center = center,
                    radius = baseRadiusPx
                ),
                radius = baseRadiusPx,
                center = center
            )
        }

        // Layer 3: Inner Knob (Rotatable)
        Box(
            modifier = Modifier
                .size(knobSize)
                .graphicsLayer { rotationZ = knobRotation }
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.3f)
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        if (isDark) listOf(DialKnobDark.copy(alpha = 0.98f), DialKnobDark)
                        else listOf(DialKnobLight, DialKnobLight.copy(alpha = 0.92f))
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onCenterClick() },
            contentAlignment = Alignment.Center
        ) {
            // Knob Ticks
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                val tickLength = 5.dp.toPx() * scaleRatio
                val tickCount = 36
                val tickColor = if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.12f)

                for (i in 0 until tickCount) {
                    val angle = (i * (360f / tickCount)) * (PI / 180f).toFloat()
                    val startX = (radius - 1.5.dp.toPx() * scaleRatio) * cos(angle) + radius
                    val startY = (radius - 1.5.dp.toPx() * scaleRatio) * sin(angle) + radius
                    val endX = (radius - tickLength - 1.5.dp.toPx() * scaleRatio) * cos(angle) + radius
                    val endY = (radius - tickLength - 1.5.dp.toPx() * scaleRatio) * sin(angle) + radius

                    drawLine(
                        color = tickColor,
                        start = androidx.compose.ui.geometry.Offset(startX, startY),
                        end = androidx.compose.ui.geometry.Offset(endX, endY),
                        strokeWidth = 1.5.dp.toPx() * scaleRatio,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Main Indicator (Orange Dot)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 6.dp * scaleRatio)
                    .size(13.dp * scaleRatio)
                    .clip(CircleShape)
                    .background(DialIndicatorOrange)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        clip = false,
                        spotColor = DialIndicatorOrange
                    )
            )

            // Center Core
            Box(
                modifier = Modifier
                    .size(centerSize)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            if (isDark) listOf(DialKnobCenterDark, DialKnobDark)
                            else listOf(DialKnobCenterLight, DialKnobLight)
                        )
                    )
            ) {
                // Sub-Indicator (Cyan Dot)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp * scaleRatio)
                        .graphicsLayer { rotationZ = subRotation }
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(9.dp * scaleRatio)
                            .clip(CircleShape)
                            .background(DialIndicatorCyan)
                            .shadow(
                                elevation = 4.dp,
                                shape = CircleShape,
                                clip = false,
                                spotColor = DialIndicatorCyan
                            )
                    )
                }
            }
        }

        // Labels / Icons around the dial
        categories.forEachIndexed { index, config ->
            val angleDeg = index * angleStep - 90.0
            val angleRad = angleDeg * PI / 180.0
            val isActive = activeCategoryIndex == index
            val iconColor = if (isActive) DialIndicatorOrange
            else if (isDark) RobotTheme.colors.textMuted
            else RobotTheme.colors.textSecondary

            Column(
                modifier = Modifier
                    .offset(
                        x = (cos(angleRad) * iconRadius).dp,
                        y = (sin(angleRad) * iconRadius).dp
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (activeCategoryIndex == index) onCenterClick()
                        else onCategoryChange(index)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp * scaleRatio)
            ) {
                Icon(
                    imageVector = config.icon,
                    contentDescription = config.label,
                    modifier = Modifier
                        .size(if (isActive) 24.dp * scaleRatio else 22.dp * scaleRatio)
                        .graphicsLayer {
                            scaleX = if (isActive) 1.15f else 1f
                            scaleY = if (isActive) 1.15f else 1f
                        },
                    tint = iconColor
                )
                Text(
                    text = config.label,
                    color = iconColor,
                    fontSize = 10.sp * scaleRatio,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
