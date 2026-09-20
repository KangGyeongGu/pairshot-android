package com.pairshot.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.pairshot.core.designsystem.PairShotRadius
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotStroke
import com.pairshot.core.designsystem.spec.PairCardSpec
import com.pairshot.core.ui.R
import kotlinx.collections.immutable.ImmutableList

data class AddPairDraft(
    val id: Long,
    val beforeUri: String? = null,
    val afterUri: String? = null,
)

enum class AddPairSlot { BEFORE, AFTER }

private const val DISABLED_SLOT_ALPHA = 0.4f
private const val INACTIVE_DOT_ALPHA = 0.3f

@Composable
fun AddPairSheet(
    drafts: ImmutableList<AddPairDraft>,
    isImporting: Boolean,
    showSaveError: Boolean,
    onPickRequest: (draftId: Long, slot: AddPairSlot) -> Unit,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filledCount = drafts.count { it.beforeUri != null }
    PairShotBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.addpair_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(PairShotSpacing.xs))
        Text(
            text = stringResource(R.string.addpair_desc_assist),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(PairShotSpacing.lg))

        val pagerState = rememberPagerState(pageCount = { drafts.size })
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = PairShotSpacing.xxxl),
            pageSpacing = PairShotSpacing.xl,
        ) { page ->
            val draft = drafts[page]
            val unlocked = page == 0 || drafts[page - 1].beforeUri != null
            AddPairDraftCard(
                draft = draft,
                enabled = !isImporting && unlocked,
                onPickRequest = onPickRequest,
                modifier = Modifier.alpha(if (unlocked) 1f else DISABLED_SLOT_ALPHA),
            )
        }
        Spacer(modifier = Modifier.height(PairShotSpacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PairShotSpacing.xs, Alignment.CenterHorizontally),
        ) {
            repeat(drafts.size) { index ->
                Box(
                    modifier =
                    Modifier
                        .size(PairShotSpacing.sm)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = INACTIVE_DOT_ALPHA)
                            },
                        ),
                )
            }
        }
        Spacer(modifier = Modifier.height(PairShotSpacing.sm))
        Text(
            text = stringResource(R.string.addpair_desc_swipe),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        if (showSaveError) {
            Spacer(modifier = Modifier.height(PairShotSpacing.sm))
            Text(
                text = stringResource(R.string.addpair_error_save_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(PairShotSpacing.lg))
        Button(
            onClick = onConfirm,
            enabled = filledCount > 0 && !isImporting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = pluralStringResource(R.plurals.addpair_button_confirm, filledCount, filledCount))
        }
    }
}

@Composable
private fun AddPairDraftCard(
    draft: AddPairDraft,
    enabled: Boolean,
    onPickRequest: (draftId: Long, slot: AddPairSlot) -> Unit,
    modifier: Modifier = Modifier,
) {
    val afterEnabled = enabled && draft.beforeUri != null
    val afterAlpha = if (draft.beforeUri != null) 1f else DISABLED_SLOT_ALPHA
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            AddPairSlotLabel(
                text = stringResource(R.string.addpair_label_before),
                modifier = Modifier.weight(1f),
            )
            AddPairSlotLabel(
                text = stringResource(R.string.addpair_label_after),
                modifier = Modifier.weight(1f).alpha(afterAlpha),
            )
        }
        Spacer(modifier = Modifier.height(PairShotSpacing.xs))
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(PairCardSpec.ASPECT_RATIO)
                .clip(RoundedCornerShape(PairShotRadius.md))
                .border(
                    width = PairShotStroke.thin,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(PairShotRadius.md),
                ),
        ) {
            AddPairSlotHalf(
                uri = draft.beforeUri,
                contentDescription = stringResource(R.string.addpair_label_before),
                enabled = enabled,
                onClick = { onPickRequest(draft.id, AddPairSlot.BEFORE) },
                modifier = Modifier.weight(1f).fillMaxSize(),
            )
            AddPairSlotHalf(
                uri = draft.afterUri,
                contentDescription = stringResource(R.string.addpair_label_after),
                enabled = afterEnabled,
                onClick = { onPickRequest(draft.id, AddPairSlot.AFTER) },
                modifier = Modifier.weight(1f).fillMaxSize().alpha(afterAlpha),
            )
        }
    }
}

@Composable
private fun AddPairSlotLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
private fun AddPairSlotHalf(
    uri: String?,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (uri != null) {
            ProfiledAsyncImage(
                data = uri,
                profile = ImageProfile.THUMBNAIL,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.AddPhotoAlternate,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
