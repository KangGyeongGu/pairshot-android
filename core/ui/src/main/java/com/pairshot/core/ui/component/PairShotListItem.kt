package com.pairshot.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun PairShotListItem(
    headline: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    trailingIsError: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier =
        if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    ListItem(
        modifier = rowModifier,
        headlineContent = {
            Text(
                text = headline,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        trailingContent =
        trailing?.let {
            {
                if (trailingIsError) {
                    WarningBadge(text = it)
                } else {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

private const val SWITCH_SCALE = 0.67f

@Composable
fun PairShotSwitchListItem(
    headline: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val rowModifier =
        if (onClick != null) modifier.clickable(enabled = enabled, onClick = onClick) else modifier
    ListItem(
        modifier = rowModifier,
        headlineContent = {
            Text(
                text = headline,
                style = MaterialTheme.typography.bodyLarge,
                color =
                if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCheckedChange(it)
                },
                enabled = enabled,
                colors =
                SwitchDefaults.colors(
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier =
                Modifier
                    .wrapContentHeight(unbounded = true)
                    .scale(SWITCH_SCALE),
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
