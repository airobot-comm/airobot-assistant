package com.airobot.framework.dial

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airobot.framework.theme.AiRobotTheme
import com.airobot.framework.theme.RobotThemeMode

private val previewCategories = listOf(
    DialCategoryConfig("AI时钟", Icons.Outlined.Schedule, 0),
    DialCategoryConfig("AI播客", Icons.Outlined.Headphones, 1),
    DialCategoryConfig("AI日程", Icons.Outlined.CalendarMonth, 2)
)

@Preview(showBackground = true, backgroundColor = 0xFFF3F7FA)
@Composable
fun SkeuomorphicDialPreviewLight() {
    AiRobotTheme(themeMode = RobotThemeMode.LIGHT) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            SkeuomorphicDial(
                categories = previewCategories,
                activeCategoryIndex = 0,
                subCategoryIndex = 0,
                subCategoryCount = 4,
                onCategoryChange = {},
                onCenterClick = {},
                modifier = Modifier.size(230.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
fun SkeuomorphicDialPreviewDark() {
    AiRobotTheme(themeMode = RobotThemeMode.DARK) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            SkeuomorphicDial(
                categories = previewCategories,
                activeCategoryIndex = 1,
                subCategoryIndex = 2,
                subCategoryCount = 5,
                onCategoryChange = {},
                onCenterClick = {},
                modifier = Modifier.size(230.dp)
            )
        }
    }
}
