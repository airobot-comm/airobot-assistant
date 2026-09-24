package com.airobot.features.podcast.cards.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airobot.framework.theme.RobotTheme

/**
 * Skeuomorphic retro record turntable component.
 * Animates record spinning and tonearm needle placement based on playing state.
 */
@Composable
fun VinylRecordTurntable(
    isPlaying: Boolean,
    rotation: Float,
    tonearmAngle: Float,
    coverResId: Int,
    cardBgColor: Color,
    onRecordClick: () -> Unit,
    modifier: Modifier = Modifier,
    playerSize: Dp = 380.dp // Renamed parameter to prevent shadowing DrawScope.size
) {
    val isDark = RobotTheme.isDark

    // Calculate proportional sub-dimensions based on the responsive size parameter
    val discSize = playerSize * 0.875f
    val labelSize = playerSize * 0.3f
    val pinHoleSize = playerSize * 0.0375f

    Box(
        modifier = modifier.size(playerSize),
        contentAlignment = Alignment.Center
    ) {
        // The black vinyl disc (spins when playing)
        Box(
            modifier = Modifier
                .size(discSize)
                .clip(CircleShape)
                .background(Color(0xFF161616))
                .border(2.dp, Color(0xFF0F0F0F), CircleShape)
                .rotate(if (isPlaying) rotation else 0f)
                .clickable { onRecordClick() },
            contentAlignment = Alignment.Center
        ) {
            // Draw realistic concentric record grooves
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val center = Offset(w / 2f, h / 2f)
                val grooveColor = Color(0xFF2E2E2E)
                val rSteps = listOf(0.92f, 0.84f, 0.76f, 0.68f, 0.60f, 0.52f)
                rSteps.forEach { step ->
                    drawCircle(
                        color = grooveColor,
                        radius = (w / 2f) * step,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            // Center disc label showing cover image and center pin hole
            Box(
                modifier = Modifier
                    .size(labelSize)
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(coverResId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.9f
                )
                // Center hole
                Box(
                    modifier = Modifier
                        .size(pinHoleSize)
                        .clip(CircleShape)
                        .background(cardBgColor)
                        .border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                )
            }
        }

        // Tonearm Stylus Arm (stationary, overlayed on top, pivot at top-right)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Pivot center position
            val x0 = w * 0.76f
            val y0 = h * 0.12f

            // Arm Shaft Length (proportional)
            val L = h * 0.53f

            // Computed end position of the arm stylus
            val rad = Math.toRadians(tonearmAngle.toDouble())
            val endX = x0 - L * Math.sin(rad).toFloat()
            val endY = y0 + L * Math.cos(rad).toFloat()

            // Draw Pivot Base (steel cylinder look)
            val pivotBaseRadius = (playerSize * 0.075f).toPx()
            val pivotInnerRadius = (playerSize * 0.040625f).toPx()
            val pivotCenterRadius = (playerSize * 0.01875f).toPx()

            drawCircle(
                color = if (isDark) Color(0xFF4A5568) else Color(0xFFB8C5CC),
                radius = pivotBaseRadius,
                center = Offset(x0, y0)
            )
            drawCircle(
                color = if (isDark) Color(0xFF2D3748) else Color(0xFF90A1AC),
                radius = pivotInnerRadius,
                center = Offset(x0, y0)
            )
            drawCircle(
                color = if (isDark) Color(0xFF718096) else Color(0xFFCBD5E1),
                radius = pivotCenterRadius,
                center = Offset(x0, y0)
            )

            // Draw Stylus Arm Shaft (silver metallic line)
            val shaftWidth = (playerSize * 0.0109375f).toPx()
            drawLine(
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFFE2E8F0),
                start = Offset(x0, y0),
                end = Offset(endX, endY),
                strokeWidth = shaftWidth,
                cap = StrokeCap.Round
            )

            // Draw stylus cartridge / head shell at the end (rotated)
            val headWidth = (playerSize * 0.04375f).toPx()
            val headHeight = (playerSize * 0.0625f).toPx()
            val cartColor = if (isDark) Color(0xFF2D3748) else Color(0xFF475569)

            // Save current canvas state to perform rotation around stylus end point
            rotate(
                degrees = -tonearmAngle + 90f,
                pivot = Offset(endX, endY)
            ) {
                // Draw head shell rect
                drawRoundRect(
                    color = cartColor,
                    topLeft = Offset(endX - headWidth / 2f, endY - headHeight * 0.7f),
                    size = androidx.compose.ui.geometry.Size(headWidth, headHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                // Small cartridge stylus point (red indicator)
                val indicatorSize = (playerSize * 0.015625f).toPx()
                drawRect(
                    color = Color(0xFFEF4444),
                    topLeft = Offset(endX - indicatorSize / 2f, endY + headHeight * 0.1f),
                    size = androidx.compose.ui.geometry.Size(indicatorSize, indicatorSize)
                )
            }
        }
    }
}
