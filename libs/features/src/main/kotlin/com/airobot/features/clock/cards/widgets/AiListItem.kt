package com.airobot.features.clock.cards.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Universal AiListItem abstraction for Clock/Timer/Focus lists.
 * Enforces unified visual appearance (shapes, backgrounds, borders, selection colors, expansion animation)
 * across all list components.
 */
@Composable
fun AiListItem(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    isEditing: Boolean,
    accentColor: Color,
    onItemClick: () -> Unit,
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    subtitle: @Composable () -> Unit,
    rightContent: @Composable RowScope.() -> Unit,
    expandedContent: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 1.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(
                if (isDark) Color.White.copy(alpha = 0.03f)
                else Color.White
            )
            .border(
                width = 1.dp,
                color = if (isEditing) accentColor.copy(alpha = 0.3f) else if (isDark) Color.Transparent else Color(
                    0xFFF1F5F9
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Column: Main Body + Expanded Content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Main Content Body Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onItemClick
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon wrapper
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }

                // Texts
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    title()
                    subtitle()
                }
            }

            // Expanded Content Panel
            AnimatedVisibility(visible = isEditing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 64.dp) // Align perfectly under the title/subtitle texts
                        .background(Color.Transparent)
                        .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    expandedContent()
                }
            }
        }

        // Right Row: Controls (Dynamically centered vertically)
        Row(
            modifier = Modifier.padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            rightContent()
        }
    }
}
