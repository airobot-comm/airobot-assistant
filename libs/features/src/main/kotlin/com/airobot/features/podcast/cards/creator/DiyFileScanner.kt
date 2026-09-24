package com.airobot.features.podcast.cards.creator

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.features.R
import com.airobot.framework.theme.PodcastFeaturedBg

data class ScannedFile(
    val name: String,
    val size: String,
    val type: String,              // "video" | "audio" | "text"
    val uri: String = "",          // Content URI string from MediaStore
    val durationMs: Long = 0L,     // Duration in milliseconds
    val mimeType: String = "",     // e.g., "audio/mpeg"
    val sizeBytes: Long = 0L       // Raw size for import
)

/**
 * Reusable skeuomorphic file scanner list component.
 * Displays a premium skeleton pulse loader during scan, a friendly empty state if no files,
 * or the scanned file list with single-selection highlight.
 */
@Composable
fun DiyFileScanner(
    isScanning: Boolean,
    scanStep: Int,
    scannedFiles: List<ScannedFile>,
    selectedFile: ScannedFile?,
    onFileSelected: (ScannedFile) -> Unit,
    isGenerating: Boolean,
    inputBgColor: Color,
    cardBorderColor: Color,
    textColor: Color,
    textMutedColor: Color,
    diyType: String,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = isScanning,
        label = "scannerStateTransition",
        modifier = modifier.fillMaxWidth()
    ) { scanning ->
        if (scanning) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(inputBgColor)
                    .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val scanTextRes = if (diyType == "video") R.string.podcast_diy_scanning_video else R.string.podcast_diy_scanning_audio
                    Text(
                        text = stringResource(scanTextRes),
                        color = PodcastFeaturedBg,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color = PodcastFeaturedBg,
                        strokeWidth = 1.5.dp
                    )
                }

                // Pulsing skeleton item placeholders
                val infiniteTransition = rememberInfiniteTransition(label = "skeletonPulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(2) { index ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Transparent)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Media Type Icon Placeholder
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            (if (diyType == "video") Color(0xFFEC4899) else Color(0xFF0EA5E9))
                                                .copy(alpha = alpha * 0.4f)
                                        )
                                )
                                // Filename placeholder
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(if (index == 0) 0.6f else 0.45f)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(textColor.copy(alpha = alpha * 0.12f))
                                )
                            }
                            // File Size placeholder
                            Box(
                                modifier = Modifier
                                    .size(width = 32.dp, height = 9.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(textMutedColor.copy(alpha = alpha * 0.12f))
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (scanStep == 2) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(inputBgColor)
                            .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "已发现可导入的文件列表 (${scannedFiles.size})",
                                color = textMutedColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "自动解析成功",
                                    color = Color(0xFF10B981),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // File items (single selection)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            scannedFiles.forEach { file ->
                                val isSelected = selectedFile == file
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) PodcastFeaturedBg.copy(alpha = 0.08f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) PodcastFeaturedBg else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable(enabled = !isGenerating) { onFileSelected(file) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (file.type == "video") Icons.Outlined.Videocam else Icons.AutoMirrored.Outlined.VolumeUp,
                                            contentDescription = null,
                                            tint = if (isSelected) PodcastFeaturedBg else (if (file.type == "video") Color(
                                                0xFFEC4899
                                            ) else Color(0xFF0EA5E9)),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = file.name,
                                            color = if (isSelected) textColor else textColor.copy(alpha = 0.8f),
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Text(
                                        text = file.size,
                                        color = if (isSelected) PodcastFeaturedBg else textMutedColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Empty state (scanStep == 0 or scanStep == 1 but no files)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(inputBgColor)
                            .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
                            .padding(vertical = 20.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val emptyTextRes = if (diyType == "video") R.string.podcast_diy_empty_video else R.string.podcast_diy_empty_audio
                        Text(
                            text = "📂 " + stringResource(emptyTextRes),
                            color = textMutedColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.podcast_diy_empty_hint),
                            color = textMutedColor.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
