package com.airobot.assistant.airobotui.framework.comp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.airobot.assistant.airobotui.framework.theme.RobotPrimaryCyan
import com.airobot.assistant.airobotui.framework.theme.RobotTextPrimary
import com.airobot.assistant.airobotui.framework.theme.RobotTextSecondary

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
            color = RobotTextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            readOnly = readOnly,
            enabled = !readOnly, // Optional: if readOnly, maybe disable interaction or keep enabled for copy? specific request "read only" usually implies interaction but no editing. 
            // User requirement: "Only display and not modify". Visual indication is important.
            // If I set readOnly=true, it is still focusable/copyable but not editable.
            // If I set enabled=false, it is grayed out.
            // Let's use readOnly = readOnly and keep enabled = true for copy, OR enabled = !readOnly.
            // Usually readOnly fields in config should look distinct or standard.
            // Let's stick to readOnly = readOnly.
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (readOnly) Color.Gray else RobotPrimaryCyan, // Visual cue
                unfocusedBorderColor = RobotTextPrimary.copy(alpha = if (readOnly) 0.05f else 0.1f),
                focusedTextColor = RobotTextPrimary,
                unfocusedTextColor = RobotTextPrimary,
                disabledTextColor = RobotTextPrimary,
                disabledBorderColor = RobotTextPrimary.copy(alpha = 0.05f)
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            trailingIcon = trailingIcon
        )
    }
}

