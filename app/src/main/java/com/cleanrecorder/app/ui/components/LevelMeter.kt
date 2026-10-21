package com.cleanrecorder.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.cleanrecorder.app.theme.AppColors

@Composable
fun LevelMeter(
    amplitude: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 28

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val centerDist = Math.abs(i - barCount / 2f) / (barCount / 2f)
            val bellFactor = (1f - centerDist * 0.5f).coerceIn(0.2f, 1f)

            val rawHeight = if (isRecording) {
                (amplitude * bellFactor * (0.8f + (i % 5) * 0.08f)).coerceIn(0.12f, 1f)
            } else {
                0.08f
            }

            val animatedHeight by animateFloatAsState(
                targetValue = rawHeight,
                animationSpec = tween(durationMillis = 70),
                label = "barHeight"
            )

            val barColor = when {
                !isRecording -> AppColors.Border
                animatedHeight > 0.75f -> AppColors.Warning
                animatedHeight > 0.45f -> AppColors.GradientMid
                else -> AppColors.Accent
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(animatedHeight)
                    .padding(horizontal = 1.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}
