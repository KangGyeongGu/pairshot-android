package com.pairshot.core.ui.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotTypographyTokens
import com.pairshot.core.designsystem.PairShotAppBar
import com.pairshot.core.designsystem.PairShotScreen
import com.pairshot.core.designsystem.PairShotRadius

private const val DISABLED_ALPHA = 0.38f

@Composable
fun PairShotActionBar(content: @Composable RowScope.() -> Unit) {
    val barColor =
        if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer
    Surface(color = barColor) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(PairShotAppBar.actionBarHeight)
                    .padding(horizontal = PairShotScreen.horizontalPadding),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PairShotSpacing.xxl, Alignment.CenterHorizontally),
                content = content,
            )
        }
    }
}

@Composable
fun PairShotActionBarItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    labelColor: Color = Color.Unspecified,
    icon: @Composable () -> Unit,
) {
    val resolvedLabelColor =
        when {
            !enabled && labelColor != Color.Unspecified -> labelColor.copy(alpha = DISABLED_ALPHA)
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
            else -> labelColor
        }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(-PairShotRadius.sm),
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            icon()
        }
        Text(
            text = label,
            style = PairShotTypographyTokens.labelExtraSmall,
            color = resolvedLabelColor,
        )
    }
}
