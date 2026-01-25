package com.airobotcomm.tablet.airobotui.subpages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobotcomm.tablet.airobotui.theme.*

@Composable
fun ConfigTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RobotPrimaryCyan,
                unfocusedBorderColor = RobotTextPrimary.copy(alpha = 0.1f),
                focusedTextColor = RobotTextPrimary,
                unfocusedTextColor = RobotTextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            trailingIcon = trailingIcon
        )
    }
}
