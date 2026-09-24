package com.airobot.features.clock.cards.chime

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airobot.features.clock.data.model.ChimeMode
import com.airobot.features.clock.data.model.HourlyChimeConfig
import com.airobot.framework.theme.DialIndicatorOrange
import com.airobot.framework.theme.RobotTheme
import com.airobot.framework.theme.StatusRed

/**
 * Skeuomorphic mechanical side-control panel mounted on the right edge of the clock chassis.
 * Implements a simple, two-button layout aligned tightly against the dial bezel (referencing Figure 2/3/4):
 * 1. 关闭/启动 (Close/Start): Mechanical toggle switch block with a red mechanical stop square or orange play arrow.
 * 2. 模式设置 (Mode Slider): Elongated slide groove track with a metallic handle sliding between 3 vertical notches (整点/单/双).
 * Markings:
 * - Position 1: 1 Solid circle notch (整点)
 * - Position 2: 1 Hollow circle notch (单点)
 * - Position 3: 2 Side-by-side circular notches (双点)
 * 100% label-free and purely metaphorical.
 */
@Composable
fun ChimeControlPanel(
    config: HourlyChimeConfig,
    isRinging: Boolean,
    onConfigChange: (HourlyChimeConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = RobotTheme.isDark

    val controlBgBrush = Brush.verticalGradient(
        listOf(
            RobotTheme.colors.skeuoControlGradientStart,
            RobotTheme.colors.skeuoControlGradientEnd
        )
    )

    val dialBgBrush = Brush.verticalGradient(
        listOf(
            RobotTheme.colors.chassisDialGradientStart,
            RobotTheme.colors.chassisDialGradientEnd
        )
    )

    val borderStrokeColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)

    Column(
        modifier = modifier
            .width(50.dp)
            .height(236.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // --- 1. Close/Start Button (关闭/启动) ---
        Box(
            modifier = Modifier
                .size(width = 50.dp, height = 88.dp)
                .clip(RoundedCornerShape(topEnd = 16.dp))
                .background(controlBgBrush)
                .border(BorderStroke(1.dp, borderStrokeColor), RoundedCornerShape(topEnd = 16.dp))
                .clickable {
                    // Toggle enabled state (also acts as dismiss if ringing)
                    onConfigChange(config.copy(enabled = !config.enabled))
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Red square (Stop/Enabled) or Orange Arrow (Play/Disabled)
                Icon(
                    imageVector = if (config.enabled || isRinging) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = "Toggle",
                    tint = if (config.enabled || isRinging) StatusRed else DialIndicatorOrange,
                    modifier = Modifier.size(28.dp)
                )

                // 3 horizontal metallic ridges
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(width = 16.dp, height = 2.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDark) Color.White.copy(alpha = 0.2f)
                                    else Color.Black.copy(alpha = 0.2f)
                                )
                        )
                    }
                }
            }
        }

        // --- 2. Mode Settings Slider (模式设置 - Elongated) ---
        Box(
            modifier = Modifier
                .size(width = 50.dp, height = 144.dp)
                .clip(RoundedCornerShape(bottomEnd = 16.dp))
                .background(dialBgBrush)
                .border(BorderStroke(1.dp, borderStrokeColor), RoundedCornerShape(bottomEnd = 16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Symmetrical Metaphorical Notch Markings (Solid/Hollow/Double)
                Column(
                    modifier = Modifier
                        .height(126.dp)
                        .padding(top = 12.dp, bottom = 12.dp, start = 2.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Position 1: 1 Solid circle notch (整)
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (config.mode == ChimeMode.EVERY_HOUR) DialIndicatorOrange 
                                else if (isDark) Color.White.copy(alpha = 0.25f) 
                                else Color.Black.copy(alpha = 0.25f)
                            )
                            .clickable { onConfigChange(config.copy(mode = ChimeMode.EVERY_HOUR)) }
                    )
                    // Position 2: 1 Hollow circle notch (单)
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .border(
                                1.5.dp, 
                                if (config.mode == ChimeMode.ODD_HOUR) DialIndicatorOrange 
                                else if (isDark) Color.White.copy(alpha = 0.25f) 
                                else Color.Black.copy(alpha = 0.25f), 
                                CircleShape
                            )
                            .clickable { onConfigChange(config.copy(mode = ChimeMode.ODD_HOUR)) }
                    )
                    // Position 3: 2 Side-by-side circular notches (双)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                        modifier = Modifier.clickable { onConfigChange(config.copy(mode = ChimeMode.EVEN_HOUR)) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (config.mode == ChimeMode.EVEN_HOUR) DialIndicatorOrange 
                                    else if (isDark) Color.White.copy(alpha = 0.25f) 
                                    else Color.Black.copy(alpha = 0.25f)
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (config.mode == ChimeMode.EVEN_HOUR) DialIndicatorOrange 
                                    else if (isDark) Color.White.copy(alpha = 0.25f) 
                                    else Color.Black.copy(alpha = 0.25f)
                                )
                        )
                    }
                }

                // Recessed track slot
                Box(
                    modifier = Modifier
                        .size(width = 20.dp, height = 126.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    // Clickable regions to slide directly
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clickable { onConfigChange(config.copy(mode = ChimeMode.EVERY_HOUR)) }
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clickable { onConfigChange(config.copy(mode = ChimeMode.ODD_HOUR)) }
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clickable { onConfigChange(config.copy(mode = ChimeMode.EVEN_HOUR)) }
                        )
                    }

                    // Sliding handle knob
                    val drumOffset by animateDpAsState(
                        targetValue = when (config.mode) {
                            ChimeMode.EVERY_HOUR -> 0.dp
                            ChimeMode.ODD_HOUR -> 52.dp
                            ChimeMode.EVEN_HOUR -> 104.dp
                        },
                        label = "drumOffset"
                    )

                    Box(
                        modifier = Modifier
                            .offset(y = drumOffset)
                            .size(width = 20.dp, height = 22.dp)
                            .shadow(4.dp, RoundedCornerShape(5.dp))
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        RobotTheme.colors.skeuoMetalGradientStart,
                                        RobotTheme.colors.skeuoMetalGradientEnd
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                if (isDark) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.6f),
                                RoundedCornerShape(5.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ridges
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 10.dp, height = 1.5.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.3f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
