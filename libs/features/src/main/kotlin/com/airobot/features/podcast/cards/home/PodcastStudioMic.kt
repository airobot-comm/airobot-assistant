package com.airobot.features.podcast.cards.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.features.R

/**
 * Studio microphone cover image inside a card container with slogan text.
 */
@Composable
fun PodcastStudioMic(
    modifier: Modifier = Modifier
) {
    val leftCardHeight = 204.dp
    val micSize = 72.dp
    Box(
        modifier = modifier
            .width(132.dp)
            .height(leftCardHeight)
            .clip(RoundedCornerShape(32.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF6366F1), Color(0xFF4338CA))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp))
            .padding(vertical = 16.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .size(micSize)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.cover_podcast_mic),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }


            Spacer(modifier = Modifier.height(14.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.podcast_studio_interactive_title),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.podcast_studio_interactive_subtitle),
                    color = Color.White.copy(alpha = 0.60f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
