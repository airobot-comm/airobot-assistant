package com.airobot.framework.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.theme.RobotTheme

@Composable
fun BottomFooter(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "·  A I R B O T   C O M M  ·",
            color = RobotTheme.colors.textPrimary.copy(alpha = 0.25f), // 极低透明度，匹配原型
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.5.sp
        )
    }
}


