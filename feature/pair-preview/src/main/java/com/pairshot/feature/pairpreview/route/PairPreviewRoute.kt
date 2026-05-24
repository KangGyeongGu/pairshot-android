package com.pairshot.feature.pairpreview.route

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pairshot.core.domain.pair.PrunePairResult
import com.pairshot.core.model.RenderProfile
import com.pairshot.core.rendering.PairImageComposer
import com.pairshot.feature.pairpreview.R
import com.pairshot.feature.pairpreview.screen.PairPreviewScreen
import com.pairshot.feature.pairpreview.viewmodel.PairPreviewUiState
import com.pairshot.feature.pairpreview.viewmodel.PairPreviewViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import timber.log.Timber

private const val MODAL_ENTER_DURATION_MS = 220

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface PairPreviewRenderEntryPoint {
    fun pairImageComposer(): PairImageComposer
}

@Composable
fun PairPreviewRoute(
    onDismiss: () -> Unit,
    onShareSelected: (pairId: Long) -> Unit,
    onNavigateToAfterCamera: (pairId: Long) -> Unit,
    onNavigateToBeforeRetake: (pairId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PairPreviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val composer =
        remember(context) {
            EntryPointAccessors
                .fromApplication(
                    context.applicationContext,
                    PairPreviewRenderEntryPoint::class.java,
                ).pairImageComposer()
        }

    val livePreviewInputs = (uiState as? PairPreviewUiState.Ready)?.livePreviewInputs

    var livePreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var livePreviewFailed by remember { mutableStateOf(false) }
    var retryToken by remember { mutableStateOf(0) }

    LaunchedEffect(livePreviewInputs, retryToken) {
        val inputs = livePreviewInputs
        val previous = livePreviewBitmap
        val next: Bitmap?
        val failed: Boolean
        val beforeUri = inputs?.pair?.beforePhotoUri
        val afterUri = inputs?.pair?.afterPhotoUri
        if (inputs == null || beforeUri == null || afterUri == null) {
            next = null
            failed = false
        } else {
            val result =
                runCatching {
                    composer.compose(
                        beforeUri = Uri.parse(beforeUri),
                        afterUri = Uri.parse(afterUri),
                        combineConfig = inputs.config,
                        watermarkConfig = inputs.watermark,
                        profile = RenderProfile.PREVIEW,
                    )
                }.onFailure { error ->
                    Timber.w(error, "live preview compose failed for pair=%d", inputs.pair.id)
                }
            next = result.getOrNull()
            failed = result.isFailure
        }
        livePreviewBitmap = next
        livePreviewFailed = failed
        if (previous != null && previous !== next && !previous.isRecycled) {
            previous.recycle()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            livePreviewBitmap?.takeIf { !it.isRecycled }?.recycle()
            livePreviewBitmap = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.deleteComplete.collect {
            onDismiss()
        }
    }

    val pruneBeforeMessage = stringResource(R.string.pair_preview_prune_notice_before)
    val pruneAfterMessage = stringResource(R.string.pair_preview_prune_notice_after)
    val pruneDeletedMessage = stringResource(R.string.pair_preview_prune_notice_deleted)
    LaunchedEffect(Unit) {
        viewModel.pruneNotice.collect { result ->
            val message =
                when (result) {
                    PrunePairResult.BeforeDropped -> pruneBeforeMessage
                    PrunePairResult.AfterDropped -> pruneAfterMessage
                    PrunePairResult.DeletedEntirely -> pruneDeletedMessage
                    PrunePairResult.NotFound, PrunePairResult.Healthy -> null
                }
            if (message != null) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter =
            scaleIn(
                initialScale = 0.94f,
                animationSpec = tween(durationMillis = MODAL_ENTER_DURATION_MS),
            ) + fadeIn(animationSpec = tween(durationMillis = MODAL_ENTER_DURATION_MS)),
        ) {
            when (val state = uiState) {
                PairPreviewUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize())
                }

                is PairPreviewUiState.Ready -> {
                    PairPreviewScreen(
                        hasCombined = state.hasCombined,
                        pairStatus = state.pair.status,
                        livePreviewBitmap = livePreviewBitmap,
                        livePreviewFailed = livePreviewFailed,
                        onLivePreviewRetry = { retryToken += 1 },
                        showDeleteDialog = state.showDeleteDialog,
                        onClose = onDismiss,
                        onShareSelected = { onShareSelected(viewModel.pairId) },
                        onNavigateToAfterCamera = { onNavigateToAfterCamera(viewModel.pairId) },
                        onNavigateToBeforeRetake = { onNavigateToBeforeRetake(viewModel.pairId) },
                        onDeleteRequested = viewModel::showDeleteDialog,
                        onDeleteAll = viewModel::deletePair,
                        onDeleteCombinedOnly = viewModel::deleteCombinedOnly,
                        onDeleteDismissed = viewModel::dismissDeleteDialog,
                    )
                }
            }
        }
    }
}
