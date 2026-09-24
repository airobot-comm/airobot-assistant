package com.airobot.features.clock.cards.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
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
 * Clickable time display widget for editing alarm time.
 *
 * Displays the current alarm time statically with a beautiful skeuomorphic bottom underline.
 * When clicked, triggers the premium [AiTimePickerDialog] allowing users to select
 * hours and minutes on a touch-optimized dial face.
 *
 * @param value The current alarm time in "HH:mm" format.
 * @param onValueChange Callback when the time is modified, emitting the validated "HH:mm" string.
 * @param accentColor The theme-specific accent color used for the skeuomorphic underline and picker elements.
 */
@Composable
fun AiSettingTimeField(
    value: String,
    onValueChange: (String) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    showUnderline: Boolean = true,
    fontSize: TextUnit = 22.sp
) {
    val textColor = RobotTheme.colors.textPrimary
    var showDialog by remember { mutableStateOf(false) }

    val boxModifier = if (showUnderline) {
        modifier
            .clickable { showDialog = true }
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
        modifier
            .clickable { showDialog = true }
    }

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = value.ifEmpty { "00:00" },
            color = textColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp
        )
    }

    if (showDialog) {
        AiTimePickerDialog(
            initialTime = value,
            onConfirm = { newTime ->
                onValueChange(newTime)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}
