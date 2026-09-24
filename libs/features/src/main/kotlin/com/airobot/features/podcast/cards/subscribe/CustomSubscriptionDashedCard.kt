package com.airobot.features.podcast.cards.subscribe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.features.R
import com.airobot.framework.theme.RobotTheme

/**
 * Dashed custom subscription placeholder card.
 */
@Composable
fun CustomSubscriptionDashedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = RobotTheme.isDark

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .shadow(
                elevation = if (isDark) 0.dp else 3.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = Color.Black.copy(alpha = 0.12f),
                ambientColor = Color.Black.copy(alpha = 0.04f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (isDark) Color(0xFF1E293B).copy(alpha = 0.3f)
                else Color(0xFFFFFFFF)
            )
            .border(
                width = 1.5.dp,
                color = RobotTheme.colors.cardBorder,
                shape = RoundedCornerShape(28.dp)
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Add custom subscription",
            tint = RobotTheme.colors.textMuted.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.podcast_subscribe_custom),
            color = RobotTheme.colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
    }
}
