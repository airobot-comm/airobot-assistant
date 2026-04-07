package com.airobot.tablet.framework.comp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.tablet.framework.theme.RobotTheme

@Composable
fun BottomFooter(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "·  A E T H E R   C O M P A N I O N  ·",
            color = RobotTheme.colors.textPrimary.copy(alpha = 0.25f), // 极低透明度，匹配原型
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )
    }
}


