package com.pairshot.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.JoinRight
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pairshot.core.designsystem.PairShotBadge
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.spec.PairCardSpec
import com.pairshot.core.model.PhotoPair
import com.pairshot.core.ui.R

private val SelectionBorderWidth = PairCardSpec.selectionBorderWidth
private val CombinedBadgeSize = PairShotBadge.size
private val CombinedBadgeIconSize = PairShotBadge.iconSize
private val CombinedBadgeIconPadding = PairShotBadge.iconPadding
private val BadgeEdgePadding = PairShotBadge.edgeInset
private const val PAIR_CARD_ASPECT_RATIO = PairCardSpec.ASPECT_RATIO

@Composable
fun PairCard(
    pair: PhotoPair,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderModifier =
        if (isSelected) {
            Modifier.border(
                width = SelectionBorderWidth,
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium,
            )
        } else {
            Modifier
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(PAIR_CARD_ASPECT_RATIO)
                .clip(MaterialTheme.shapes.medium)
                .then(borderModifier)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongPress,
                ),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            PairCardSlot(
                uri = pair.beforePhotoUri,
                contentDescription = stringResource(R.string.pair_card_desc_before),
                modifier = Modifier.weight(1f).fillMaxSize(),
            )
            PairCardSlot(
                uri = pair.afterPhotoUri,
                contentDescription = stringResource(R.string.pair_card_desc_after),
                modifier = Modifier.weight(1f).fillMaxSize(),
            )
        }

        if (pair.hasCombined) {
            CombinedStatusBadge(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = BadgeEdgePadding, top = BadgeEdgePadding),
            )
        }

        if (isSelectionMode && isSelected) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            )
        }
    }
}

@Composable
private fun PairCardSlot(
    uri: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    if (uri.isNullOrBlank()) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.CameraAlt,
                contentDescription = stringResource(R.string.pair_card_desc_scheduled),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        }
    } else {
        ProfiledAsyncImage(
            data = uri,
            profile = ImageProfile.THUMBNAIL,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

@Composable
private fun CombinedStatusBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(CombinedBadgeSize),
        shape = RoundedCornerShape(PairShotSpacing.sm),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Icon(
            imageVector = Icons.Filled.JoinRight,
            contentDescription = stringResource(R.string.pair_card_desc_combined),
            modifier = Modifier.padding(CombinedBadgeIconPadding).size(CombinedBadgeIconSize),
        )
    }
}
