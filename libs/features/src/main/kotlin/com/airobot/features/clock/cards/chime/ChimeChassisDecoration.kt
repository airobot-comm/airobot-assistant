package com.airobot.features.clock.cards.chime

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.features.clock.data.model.HourlyChimeConfig
import com.airobot.framework.theme.DialIndicatorOrange
import com.airobot.framework.theme.RobotTheme

/**
 * Skeuomorphic mechanical Cuckoo Clock chassis ears decoration.
 * Optimizations:
 * 1. Enhances visual contrast under light theme using a dark slate outline profile.
 * 2. Cuckoo Clock Houses remain stationary (no vertical bounce/vibrations on the house itself).
 * 3. Ringing animation is strictly shutter sliding open/close and popping Cuckoo Bird cuckooing.
 */
@Composable
fun ChimeChassisDecoration(
    config: HourlyChimeConfig,
    isRinging: Boolean,
    bellRotation: Float,
    isDark: Boolean,
    onConfigChange: (HourlyChimeConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 1. Top Metal Hammer Link (Centered on top of the dial bezel)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 40.dp)
                .size(32.dp, 54.dp)
                .shadow(4.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            RobotTheme.colors.skeuoMetalGradientStart,
                            RobotTheme.colors.skeuoMetalGradientEnd
                        )
                    )
                )
                .border(
                    1.dp,
                    RobotTheme.colors.chassisBorderSubtle,
                    RoundedCornerShape(8.dp)
                )
        )

        // 2. Interactive Metallic Cuckoo Clock "Ears"
        val houseBrush = if (isDark) {
            Brush.linearGradient(
                listOf(
                    RobotTheme.colors.skeuoMetalGradientStart,
                    RobotTheme.colors.skeuoMetalGradientEnd
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color(0xFFF8FAFC), // Ultra-clean brushed steel highlight
                    Color(0xFFE2E8F0),
                    Color(0xFFCBD5E1)
                )
            )
        }

        val borderBrush = if (isDark) {
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent))
        } else {
            // Highly pronounced, sharp slate borders for perfect light theme clarity
            Brush.verticalGradient(listOf(Color(0xFF475569), Color(0xFF1E293B)))
        }

        val plaqueBg = if (isDark) Color.Black.copy(alpha = 0.25f) else Color(0xFF334155) // Slate-700
        val plaqueBorder = if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFF475569)

        // Left Cuckoo Ear (Start Hour Controller)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = (-138).dp, y = 28.dp) // Stationary house
                .size(102.dp, 80.dp)
                .shadow(6.dp, RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                .clip(RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(houseBrush)
                .border(
                    width = 1.5.dp,
                    brush = borderBrush,
                    shape = RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                )
                .clickable {
                    val nextHour = (config.startHour + 1) % 24
                    onConfigChange(config.copy(startHour = nextHour))
                },
            contentAlignment = Alignment.Center
        ) {
            // Recessed Arched Window slot cavity
            Box(
                modifier = Modifier
                    .offset(y = (-6).dp)
                    .size(width = 54.dp, height = 44.dp)
                    .clip(RoundedCornerShape(topStart = 27.dp, topEnd = 27.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(Color(0xFF1E293B))
                    .border(
                        1.5.dp,
                        if (isDark) Color.Black else Color(0xFF0F172A),
                        RoundedCornerShape(topStart = 27.dp, topEnd = 27.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    )
            ) {
                // Popping Cuckoo Bird (Gentle bobbing)
                val birdBounce = if (isRinging) bellRotation * 0.8f else 0f
                val birdScale = if (isRinging) 1f else 0f
                
                CuckooBird(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = birdBounce.dp)
                        .graphicsLayer(
                            scaleX = birdScale,
                            scaleY = birdScale,
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        )
                )

                // Shutter Doors
                val shutterLeftOffset by animateDpAsState(targetValue = if (isRinging) (-28).dp else 0.dp, label = "shutterL")
                val shutterRightOffset by animateDpAsState(targetValue = if (isRinging) 28.dp else 0.dp, label = "shutterR")

                // Left Shutter
                Box(
                    modifier = Modifier
                        .offset(x = shutterLeftOffset)
                        .width(27.dp)
                        .fillMaxHeight()
                        .align(Alignment.CenterStart)
                        .background(houseBrush)
                        .border(0.5.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.15f))
                )
                
                // Right Shutter
                Box(
                    modifier = Modifier
                        .offset(x = shutterRightOffset)
                        .width(27.dp)
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .background(houseBrush)
                        .border(0.5.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.15f))
                )
            }

            // Engraved Digital plaque below cuckoo window (High-contrast tags)
            Box(
                modifier = Modifier
                    .offset(y = 22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(plaqueBg)
                    .border(0.5.dp, plaqueBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = String.format("%02d:00", config.startHour),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DialIndicatorOrange,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        // Right Cuckoo Ear (End Hour Controller)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 138.dp, y = 28.dp) // Stationary house
                .size(102.dp, 80.dp)
                .shadow(6.dp, RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                .clip(RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(houseBrush)
                .border(
                    width = 1.5.dp,
                    brush = borderBrush,
                    shape = RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                )
                .clickable {
                    val nextHour = (config.endHour + 1) % 24
                    onConfigChange(config.copy(endHour = nextHour))
                },
            contentAlignment = Alignment.Center
        ) {
            // Recessed Arched Window slot cavity
            Box(
                modifier = Modifier
                    .offset(y = (-6).dp)
                    .size(width = 54.dp, height = 44.dp)
                    .clip(RoundedCornerShape(topStart = 27.dp, topEnd = 27.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(Color(0xFF1E293B))
                    .border(
                        1.5.dp,
                        if (isDark) Color.Black else Color(0xFF0F172A),
                        RoundedCornerShape(topStart = 27.dp, topEnd = 27.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    )
            ) {
                // Popping Cuckoo Bird (Gentle bobbing)
                val birdBounce = if (isRinging) -bellRotation * 0.8f else 0f
                val birdScale = if (isRinging) 1f else 0f
                
                CuckooBird(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = birdBounce.dp)
                        .graphicsLayer(
                            scaleX = birdScale,
                            scaleY = birdScale,
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        )
                )

                // Shutter Doors
                val shutterLeftOffset by animateDpAsState(targetValue = if (isRinging) (-28).dp else 0.dp, label = "shutterL")
                val shutterRightOffset by animateDpAsState(targetValue = if (isRinging) 28.dp else 0.dp, label = "shutterR")

                // Left Shutter
                Box(
                    modifier = Modifier
                        .offset(x = shutterLeftOffset)
                        .width(27.dp)
                        .fillMaxHeight()
                        .align(Alignment.CenterStart)
                        .background(houseBrush)
                        .border(0.5.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.15f))
                )
                
                // Right Shutter
                Box(
                    modifier = Modifier
                        .offset(x = shutterRightOffset)
                        .width(27.dp)
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .background(houseBrush)
                        .border(0.5.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.15f))
                )
            }

            // Engraved Digital plaque below cuckoo window (High-contrast tags)
            Box(
                modifier = Modifier
                    .offset(y = 22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(plaqueBg)
                    .border(0.5.dp, plaqueBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = String.format("%02d:00", config.endHour),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DialIndicatorOrange,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * A beautiful skeuomorphic Vector Cuckoo Bird drawn using Compose Canvas.
 */
@Composable
fun CuckooBird(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(36.dp)) {
        val width = size.width
        val height = size.height

        // 1. Body (Amber/Orange circle)
        drawCircle(
            color = Color(0xFFF97316),
            radius = width * 0.32f,
            center = Offset(width * 0.45f, height * 0.58f)
        )

        // 2. Head (Light Orange smaller circle)
        drawCircle(
            color = Color(0xFFFB923C),
            radius = width * 0.22f,
            center = Offset(width * 0.65f, height * 0.36f)
        )

        // 3. Beak (Golden Yellow triangle pointing forward)
        val beakPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(width * 0.82f, height * 0.30f)
            lineTo(width * 0.98f, height * 0.36f)
            lineTo(width * 0.82f, height * 0.42f)
            close()
        }
        drawPath(
            path = beakPath,
            color = Color(0xFFFBBF24)
        )

        // 4. Eye (Sharp black pupil with small white reflection)
        drawCircle(
            color = Color.Black,
            radius = width * 0.045f,
            center = Offset(width * 0.68f, height * 0.32f)
        )
        drawCircle(
            color = Color.White,
            radius = width * 0.015f,
            center = Offset(width * 0.70f, height * 0.30f)
        )

        // 5. Wing (Soft light yellow feather accent)
        val wingPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(width * 0.25f, height * 0.52f)
            quadraticTo(
                width * 0.40f, height * 0.40f,
                width * 0.55f, height * 0.55f
            )
            quadraticTo(
                width * 0.35f, height * 0.70f,
                width * 0.25f, height * 0.52f
            )
            close()
        }
        drawPath(
            path = wingPath,
            color = Color(0xFFFEF08A)
        )
    }
}
