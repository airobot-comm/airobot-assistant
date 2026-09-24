package com.airobot.features.clock.cards.widgets

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.theme.RobotTheme

/**
 * A beautiful, compact, and premium skeuomorphic drum / wheel time picker dialog.
 * Resolves screen overflow in landscape viewports by presenting a vertical scrolling layout,
 * allowing hours and minutes to be flicked/dialed individually with stunning 3D cylindrical rolls.
 * Utilizes the unified [SkeuomorphicDrumPicker] component.
 *
 * @param initialTime The starting time formatted as "HH:mm".
 * @param onConfirm Callback when "确定" is clicked, returning the selected time formatted as "HH:mm".
 * @param onDismiss Callback when the dialog is dismissed or "取消" is clicked.
 */
@Composable
fun AiTimePickerDialog(
    initialTime: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Parse the initial hour and minute with safe fallback values
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val formatted = String.format("%02d:%02d", selectedHour, selectedMinute)
                    onConfirm(formatted)
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
                    text = "选择时间",
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
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour Rolling Drum using unified SkeuomorphicDrumPicker
                SkeuomorphicDrumPicker(
                    minVal = 0,
                    maxVal = 23,
                    value = selectedHour,
                    onValueChange = { selectedHour = it },
                    width = 80.dp
                )

                // Breathing/Glow Separator `:`
                Text(
                    text = ":",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = RobotTheme.colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Minute Rolling Drum using unified SkeuomorphicDrumPicker
                SkeuomorphicDrumPicker(
                    minVal = 0,
                    maxVal = 59,
                    value = selectedMinute,
                    onValueChange = { selectedMinute = it },
                    width = 80.dp
                )
            }
        },
        containerColor = RobotTheme.colors.cardBg,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.width(280.dp) // Ensure compact portrait slot size
    )
}
