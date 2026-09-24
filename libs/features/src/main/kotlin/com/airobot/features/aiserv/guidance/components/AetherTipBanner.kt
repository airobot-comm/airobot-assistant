package com.airobot.features.aiserv.guidance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.theme.RobotTheme

/**
 * Aether Tip Banner — a single-page static internal prompt component to guide users on function usage.
 * Encapsulated within the features module under open-closed principles.
 */
@Composable
internal fun AetherTipBanner(
    promptText: String,
    modifier: Modifier = Modifier
) {
    val cardBgColor = RobotTheme.colors.aetherTipBg
    val cardBorderColor = RobotTheme.colors.aetherTipBorder
    val brandAccent = RobotTheme.colors.aetherTipAccent
    val iconBgColor = RobotTheme.colors.aetherTipIconBg

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .offset(y = (-8).dp)
            .clip(RoundedCornerShape(40.dp))
            .background(cardBgColor)
            .border(
                width = 1.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(40.dp)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left brain icon circle container
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Psychology,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = brandAccent
            )
        }

        // Text content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header label: "AETHER 提示"
            Text(
                text = "AETHER 提示",
                color = brandAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            // Description body
            Text(
                text = promptText,
                color = RobotTheme.colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp
            )
        }
    }
}
