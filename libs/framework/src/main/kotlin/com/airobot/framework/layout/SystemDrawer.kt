package com.airobot.framework.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.R
import com.airobot.framework.theme.RobotTheme
import com.airobot.framework.util.LanguageMode

data class DrawerMenuItemData(
    val icon: ImageVector,
    val label: String,
    val title: String,
    val content: @Composable () -> Unit
)

@Composable
fun SystemDrawer(
    menuItems: List<DrawerMenuItemData>,
    languageMode: LanguageMode = LanguageMode.SYSTEM,
    onClose: () -> Unit,
    onToggleTheme: () -> Unit = {},
    onLanguageChange: (LanguageMode) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(640.dp),
        color = RobotTheme.colors.background,
        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .background(RobotTheme.colors.surfaceOverlay.copy(alpha = if (RobotTheme.isDark) 0.05f else 0.1f))
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Scrollable container for the main menu items
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    menuItems.forEachIndexed { index, item ->
                        DrawerMenuItem(
                            icon = item.icon,
                            label = item.label,
                            isSelected = selectedTab == index,
                            onClick = { selectedTab = index }
                        )
                        if (index < menuItems.lastIndex) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DrawerMenuItem(
                    icon = Icons.Default.Language,
                    label = when (languageMode) {
                        LanguageMode.SYSTEM -> stringResource(R.string.system_default)
                        LanguageMode.ENGLISH -> stringResource(R.string.lang_english)
                        LanguageMode.CHINESE -> stringResource(R.string.lang_chinese)
                    },
                    isSelected = false,
                    onClick = {
                        val nextMode = when (languageMode) {
                            LanguageMode.SYSTEM -> LanguageMode.CHINESE
                            LanguageMode.CHINESE -> LanguageMode.ENGLISH
                            LanguageMode.ENGLISH -> LanguageMode.SYSTEM
                        }
                        onLanguageChange(nextMode)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                DrawerMenuItem(
                    icon = if (RobotTheme.isDark) Icons.Default.WbSunny else Icons.Default.NightsStay,
                    label = if (RobotTheme.isDark) stringResource(R.string.light_mode) else stringResource(
                        R.string.dark_mode
                    ),
                    isSelected = false,
                    onClick = onToggleTheme
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val currentItem = menuItems.getOrNull(selectedTab)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = currentItem?.title ?: "",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = RobotTheme.colors.textPrimary,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(width = 40.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(RobotTheme.colors.accent)
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(RobotTheme.colors.surfaceOverlay.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = RobotTheme.colors.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    currentItem?.content?.invoke()
                }
            }
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor =
        if (isSelected) RobotTheme.colors.accent.copy(alpha = 0.15f) else Color.Transparent
    val contentColor = if (isSelected) RobotTheme.colors.accent else RobotTheme.colors.textMuted

    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
