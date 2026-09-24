package com.airobot.features.clock.cards.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.theme.RobotTheme

/**
 * Clickable duration display widget for editing timer preset minutes.
 *
 * Displays the current minutes statically with a beautiful skeuomorphic bottom underline.
 * When clicked, triggers [AiDurationPickerDialog] allowing users to select minutes (1-180)
 * on a touch-optimized 3D cylindrical drum rolling wheel.
 *
 * @param valueMinutes The current minutes value (e.g. 25).
 * @param onValueChange Callback when the minutes are modified.
 * @param accentColor The theme-specific accent color.
 */
@Composable
fun AiSettingDurationField(
    valueMinutes: Int,
    onValueChange: (Int) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    showUnderline: Boolean = true,
    fontSize: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.Black
) {
    val textColor = RobotTheme.colors.textPrimary
    var showDialog by remember { mutableStateOf(false) }

    val underlineModifier = if (showUnderline) {
        Modifier
            .drawBehind {
                val y = size.height - 2.dp.toPx()
                drawLine(
                    color = accentColor.copy(alpha = 0.3f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(bottom = 2.dp)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clickable { showDialog = true }
            .then(underlineModifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = String.format("%02d", valueMinutes),
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            letterSpacing = if (fontSize == 20.sp) (-0.5).sp else 0.sp
        )
    }

    if (showDialog) {
        AiDurationPickerDialog(
            initialMinutes = valueMinutes,
            onConfirm = { newMinutes ->
                onValueChange(newMinutes)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * Skeuomorphic 3D drum rolling wheel picker dialog for duration selection.
 */
@Composable
fun AiDurationPickerDialog(
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMinutes by remember { mutableStateOf(initialMinutes.coerceIn(1, 180)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selectedMinutes)
                }
            ) {
                Text(
                    text = "确定",
                    color = RobotTheme.colors.accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "取消",
                    color = RobotTheme.colors.textMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        },
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "选择时长",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = RobotTheme.colors.textPrimary
                )
            }
        },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minute Drum using unified SkeuomorphicDrumPicker
                SkeuomorphicDrumPicker(
                    minVal = 1,
                    maxVal = 180,
                    value = selectedMinutes,
                    onValueChange = { selectedMinutes = it },
                    width = 90.dp
                )

                Text(
                    text = "分钟",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = RobotTheme.colors.textPrimary,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        },
        containerColor = RobotTheme.colors.cardBg,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.width(280.dp)
    )
}
