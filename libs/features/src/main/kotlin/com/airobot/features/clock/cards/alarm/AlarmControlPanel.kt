package com.airobot.features.clock.cards.alarm

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airobot.features.clock.data.model.AlarmItem
import com.airobot.framework.cards.OverlayCloseButton
import com.airobot.framework.cards.OverlayCloseMode
import com.airobot.framework.theme.DialIndicatorOrange
import com.airobot.framework.theme.RobotTheme
import com.airobot.framework.theme.StatusRed

/**
 * Renders the top power shutoff and right slim mechanical button panels.
 */
@Composable
fun AlarmControlPanel(
    alarm: AlarmItem,
    isRinging: Boolean,
    isDark: Boolean,
    onDismissAlarm: () -> Unit,
    onToggleAlarm: () -> Unit,
    onMinimizeAlarm: (Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 3. Top Stop/Power Button (sitting on top of the dial bezel)
        OverlayCloseButton(
            closeMode = if (isRinging) OverlayCloseMode.TRIPLE_CONFIRM else OverlayCloseMode.SIMPLE,
            onClose = {
                if (isRinging) {
                    onDismissAlarm()
                } else {
                    onClose()
                }
            },
            accentColor = RobotTheme.colors.alarmAccent,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 47.dp)
        )

        // 5. Right-side Mechanical Control Panel (Slimmer buttons with larger icons)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 12.dp, y = 48.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Ringing toggle/stop switch (top right)
            Box(
                modifier = Modifier
                    .size(width = 50.dp, height = 88.dp)
                    .clip(RoundedCornerShape(topEnd = 16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                RobotTheme.colors.skeuoControlGradientStart,
                                RobotTheme.colors.skeuoControlGradientEnd
                            )
                        )
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
                        ),
                        RoundedCornerShape(topEnd = 16.dp)
                    )
                    .clickable {
                        if (isRinging) {
                            onMinimizeAlarm(true)
                        } else {
                            onToggleAlarm()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isRinging || alarm.enabled) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Toggle",
                        tint = if (isRinging || alarm.enabled) StatusRed else DialIndicatorOrange,
                        modifier = Modifier.size(28.dp)
                    )
                    // Grip lines
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

            // Minimize switch (bottom right)
            Box(
                modifier = Modifier
                    .size(width = 50.dp, height = 62.dp)
                    .clip(RoundedCornerShape(bottomEnd = 16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                RobotTheme.colors.chassisDialGradientStart,
                                RobotTheme.colors.chassisDialGradientEnd
                            )
                        )
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
                        ),
                        RoundedCornerShape(bottomEnd = 16.dp)
                    )
                    .clickable {
                        if (isRinging) {
                            onMinimizeAlarm(false)
                        } else {
                            onClose()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FullscreenExit,
                    contentDescription = "Minimize",
                    tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
