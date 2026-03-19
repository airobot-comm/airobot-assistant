package com.airobot.tablet.airobotui.framework.comp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.airobot.tablet.airobotui.framework.theme.RobotTheme

@Composable
fun ConfigTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = RobotTheme.colors.textSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            readOnly = readOnly,
            enabled = !readOnly,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (readOnly) Color.Gray else RobotTheme.colors.accent,
                unfocusedBorderColor = RobotTheme.colors.textPrimary.copy(alpha = if (readOnly) 0.05f else 0.1f),
                focusedTextColor = RobotTheme.colors.textPrimary,
                unfocusedTextColor = RobotTheme.colors.textPrimary,
                disabledTextColor = RobotTheme.colors.textPrimary,
                disabledBorderColor = RobotTheme.colors.textPrimary.copy(alpha = 0.05f)
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            trailingIcon = trailingIcon
        )
    }
}


