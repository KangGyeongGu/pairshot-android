package com.pairshot.core.ui.component

import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pairshot.core.designsystem.PairShotSpacing

@Composable
fun MarqueeTitleText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier =
            modifier.basicMarquee(
                iterations = Int.MAX_VALUE,
                animationMode = MarqueeAnimationMode.Immediately,
                repeatDelayMillis = 1_200,
                velocity = PairShotSpacing.xxl,
            ),
    )
}
