package com.pairshot.feature.exportsettings.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pairshot.core.designsystem.PairShotCard
import com.pairshot.core.designsystem.PairShotRadius
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.domain.tutorial.AnchorKey
import com.pairshot.core.model.ExportPresetSlot
import com.pairshot.core.ui.component.SettingsCard
import com.pairshot.feature.exportsettings.R
import com.pairshot.feature.tutorial.ui.modifier.tutorialAnchor
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ExportPresetSection(
    slots: ImmutableList<ExportPresetSlot>,
    activeSlotId: String,
    isProSubscriber: Boolean,
    onSelectSlot: (String) -> Unit,
    onLongPressSlot: (ExportPresetSlot) -> Unit,
    onAddSlot: () -> Unit,
    onProLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCard(modifier = modifier) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(PairShotCard.innerPadding),
            horizontalArrangement = Arrangement.spacedBy(PairShotSpacing.sm),
        ) {
            for (index in 0 until ExportPresetSlot.MAX_SLOTS) {
                val slot = slots.getOrNull(index)
                val locked = !isProSubscriber && index >= ExportPresetSlot.FREE_SLOT_LIMIT
                val anchor = if (index == 0) AnchorKey.EXPORT_PRESET_DEFAULT_CARD else null
                PresetSlotCell(
                    modifier =
                    Modifier
                        .weight(1f)
                        .let { base ->
                            if (anchor != null) base.tutorialAnchor(anchor) else base
                        },
                    slot = slot,
                    selected = slot != null && slot.id == activeSlotId,
                    locked = locked,
                    onSelect = {
                        when {
                            locked -> onProLock()
                            slot != null -> onSelectSlot(slot.id)
                            else -> onAddSlot()
                        }
                    },
                    onLongPress = {
                        if (!locked && slot != null) onLongPressSlot(slot)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetSlotCell(
    slot: ExportPresetSlot?,
    selected: Boolean,
    locked: Boolean,
    onSelect: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val effectiveSelected = selected && !locked
    val borderColor =
        when {
            effectiveSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outlineVariant
        }
    val containerColor =
        when {
            locked -> Color.Transparent
            slot == null -> Color.Transparent
            effectiveSelected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        }
    val shape = RoundedCornerShape(PairShotRadius.md)
    val cellModifier =
        modifier
            .aspectRatio(SLOT_ASPECT_RATIO)
            .clip(shape)
            .border(
                width = if (effectiveSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = shape,
            )
            .combinedClickable(onClick = onSelect, onLongClick = onLongPress)
    Surface(
        modifier = cellModifier,
        color = containerColor,
    ) {
        when {
            locked -> LockedSlot(slotName = slot?.name)
            slot != null -> FilledSlot(name = slot.name, selected = effectiveSelected)
            else -> AddSlot()
        }
    }
}

@Composable
private fun FilledSlot(
    name: String,
    selected: Boolean,
) {
    val textColor =
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier.padding(PairShotSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AddSlot() {
    Box(
        modifier = Modifier.padding(PairShotSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = stringResource(R.string.export_preset_add_desc),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LockedSlot(slotName: String?) {
    Column(
        modifier = Modifier.padding(PairShotSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PairShotSpacing.xs, Alignment.CenterVertically),
    ) {
        if (slotName.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = stringResource(R.string.export_preset_add_locked_desc),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(LOCK_ICON_SIZE),
            )
        } else {
            Text(
                text = slotName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = LOCKED_NAME_ALPHA),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SmallProBadge()
    }
}

@Composable
private fun SmallProBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(PRO_BADGE_RADIUS))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(
            text = PRO_LABEL,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = PRO_BADGE_FONT_SIZE,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

private const val SLOT_ASPECT_RATIO = 1f
private const val LOCKED_NAME_ALPHA = 0.4f
private val LOCK_ICON_SIZE = 16.dp
private val PRO_BADGE_RADIUS = 3.dp
private val PRO_BADGE_FONT_SIZE = 9.sp
private const val PRO_LABEL = "PRO"
