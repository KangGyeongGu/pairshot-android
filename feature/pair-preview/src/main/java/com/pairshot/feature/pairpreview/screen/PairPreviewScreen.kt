package com.pairshot.feature.pairpreview.screen

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pairshot.core.adsui.component.PairShotBannerAd
import com.pairshot.core.designsystem.PairShotDialogTokens
import com.pairshot.core.model.PairStatus
import com.pairshot.core.ui.component.DeletePairConfirmDialog
import com.pairshot.feature.pairpreview.component.MissingSlotPlaceholder
import com.pairshot.feature.pairpreview.component.MissingSlotSide
import com.pairshot.feature.pairpreview.component.PairPreviewCenter
import com.pairshot.feature.pairpreview.component.PairPreviewTopBar

@Composable
fun PairPreviewScreen(
    hasCombined: Boolean,
    pairStatus: PairStatus,
    livePreviewBitmap: Bitmap?,
    livePreviewFailed: Boolean,
    onLivePreviewRetry: () -> Unit,
    showDeleteDialog: Boolean,
    onClose: () -> Unit,
    onShareSelected: () -> Unit,
    onNavigateToAfterCamera: () -> Unit,
    onNavigateToBeforeRetake: () -> Unit,
    onDeleteRequested: () -> Unit,
    onDeleteAll: () -> Unit,
    onDeleteCombinedOnly: () -> Unit,
    onDeleteDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier =
            Modifier.size(
                width = PairShotDialogTokens.width,
                height = PairShotDialogTokens.height,
            ),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = PairShotDialogTokens.elevation,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                PairPreviewTopBar(
                    onClose = onClose,
                    onShareSelected = onShareSelected,
                    onNavigateToAfterCamera = onNavigateToAfterCamera,
                    onDeleteRequested = onDeleteRequested,
                    modifier = Modifier.fillMaxWidth(),
                )

                Box(modifier = Modifier.weight(1f)) {
                    when (pairStatus) {
                        PairStatus.PAIRED -> {
                            PairPreviewCenter(
                                livePreviewBitmap = livePreviewBitmap,
                                livePreviewFailed = livePreviewFailed,
                                onRetry = onLivePreviewRetry,
                            )
                        }

                        PairStatus.BEFORE_ONLY -> {
                            MissingSlotPlaceholder(
                                side = MissingSlotSide.AFTER,
                                onCapture = onNavigateToAfterCamera,
                            )
                        }

                        PairStatus.AFTER_ONLY -> {
                            MissingSlotPlaceholder(
                                side = MissingSlotSide.BEFORE,
                                onCapture = onNavigateToBeforeRetake,
                            )
                        }
                    }
                }

                PairShotBannerAd()
            }
        }
    }

    if (showDeleteDialog) {
        DeletePairConfirmDialog(
            pairCount = 1,
            combinedCount = if (hasCombined) 1 else 0,
            onDeleteAll = onDeleteAll,
            onDeleteCombinedOnly = onDeleteCombinedOnly,
            onDismiss = onDeleteDismissed,
        )
    }
}
