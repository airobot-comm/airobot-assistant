package com.airobot.features.clock.cards.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import android.widget.Toast
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airobot.framework.theme.StatusAmber

/**
 * TimerControlPanel — renders the right-side play/pause and minimize/exit skeuomorphic buttons.
 */
@Composable
fun BoxScope.TimerControlPanel(
    isFocusMode: Boolean,
    isDark: Boolean,
    isRunning: Boolean,
    accentColor: Color,
    onToggle: () -> Unit,
    onSendToBackground: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    isFinished: Boolean = false,
    onShowConstraintAlert: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .align(Alignment.CenterEnd)
            .offset(x = 12.dp, y = 48.dp)
            .width(50.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Play / Pause Toggle Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .shadow(6.dp, RoundedCornerShape(topEnd = 16.dp))
                .clip(RoundedCornerShape(topEnd = 16.dp))
                .background(
                    Brush.verticalGradient(
                        if (isDark) listOf(Color(0xFF0E7490), Color(0xFF0891B2))
                        else listOf(Color.White, Color(0xFFECFEFF))
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = if (isDark) Color(0xFF22D3EE).copy(alpha = 0.3f) else Color(0xFFBAE6FD),
                    shape = RoundedCornerShape(topEnd = 16.dp)
                )
                .clickable {
                    if (isFocusMode && isRunning) {
                        onShowConstraintAlert("专注模式不可切换")
                    } else {
                        onToggle()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isFocusMode && isRunning) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = if (isDark) Color(0xFFF87171) else Color(0xFFEF4444),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 6.dp, y = 6.dp)
                        .size(12.dp)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Toggle",
                    tint = if (isRunning) StatusAmber else accentColor,
                    modifier = Modifier.size(28.dp)
                )
                // Grip lines
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(width = 16.dp, height = 2.dp)
                                .clip(CircleShape)
                                .background((if (isDark) Color.White else Color.Black).copy(alpha = 0.2f))
                        )
                    }
                }
            }
        }

        // Minimize / Send to Background Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .shadow(6.dp, RoundedCornerShape(bottomEnd = 16.dp))
                .clip(RoundedCornerShape(bottomEnd = 16.dp))
                .background(
                    Brush.verticalGradient(
                        if (isDark) listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        else listOf(Color.White, Color(0xFFF1F5F9))
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(bottomEnd = 16.dp)
                )
                .clickable {
                    if (isFocusMode) {
                        if (isRunning) {
                            onShowConstraintAlert("专注模式不可切换")
                        } else {
                            onReset()
                            onClose()
                        }
                    } else {
                        if (isFinished) {
                            onReset()
                            onClose()
                        } else {
                            onSendToBackground()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isFocusMode && isRunning) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = if (isDark) Color(0xFFF87171) else Color(0xFFEF4444),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 6.dp, y = 6.dp)
                        .size(12.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.FullscreenExit,
                contentDescription = "Minimize",
                tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

