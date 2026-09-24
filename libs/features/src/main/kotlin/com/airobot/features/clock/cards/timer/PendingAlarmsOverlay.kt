package com.airobot.features.clock.cards.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.features.R
import com.airobot.features.clock.data.model.AlarmItem
import com.airobot.framework.cards.OverlayBackdrop
import com.airobot.framework.theme.RobotTheme

/**
 * PendingAlarmsOverlay — renders the suppressed/queued alarms list popup overlay.
 */
@Composable
fun PendingAlarmsOverlay(
    pendingAlarms: List<AlarmItem>,
    accentColor: Color,
    onConfirmAndClear: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    OverlayBackdrop(
        onClose = onClose
    ) {
        Box(
            modifier = modifier
                .size(width = 300.dp, height = 360.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(RobotTheme.colors.cardBg)
                .border(2.dp, accentColor, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.pending_alarms_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = RobotTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pendingAlarms.forEach { alarm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(RobotTheme.colors.chassisButtonBg)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = alarm.time,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RobotTheme.colors.textPrimary
                                )
                                Text(
                                    text = alarm.label,
                                    fontSize = 11.sp,
                                    color = RobotTheme.colors.textMuted
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.pending_alarms_untriggered),
                                    color = Color(0xFFEF4444),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor)
                        .clickable {
                            onConfirmAndClear()
                            onClose()
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.pending_alarms_confirm),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
