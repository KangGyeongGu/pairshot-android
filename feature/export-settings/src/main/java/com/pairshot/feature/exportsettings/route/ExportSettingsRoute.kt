package com.pairshot.feature.exportsettings.route

import android.widget.Toast
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pairshot.core.model.ExportPresetSlot
import com.pairshot.core.navigation.PaywallTrigger
import com.pairshot.feature.exportsettings.R
import com.pairshot.feature.exportsettings.component.PresetActionsDialog
import com.pairshot.feature.exportsettings.component.PresetNameDialog
import com.pairshot.feature.exportsettings.screen.ExportSettingsScreen
import com.pairshot.feature.exportsettings.viewmodel.ExportSettingsViewModel
import com.pairshot.feature.exportsettings.viewmodel.SlotError
import com.pairshot.feature.tutorial.domain.TutorialSection
import com.pairshot.feature.tutorial.domain.TutorialStepId
import dagger.hilt.android.EntryPointAccessors

@Composable
@Suppress("LongMethod")
fun ExportSettingsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToWatermarkSettings: () -> Unit,
    onNavigateToCombineSettings: () -> Unit,
    onNavigateToPaywall: (PaywallTrigger) -> Unit,
    onShare: (Set<Long>) -> Unit,
    onSaveToDevice: (Set<Long>) -> Unit,
    viewModel: ExportSettingsViewModel = hiltViewModel(),
) {
    val preset by viewModel.effectivePreset.collectAsStateWithLifecycle()
    val watermarkConfig by viewModel.effectiveWatermarkConfig.collectAsStateWithLifecycle()
    val applyWatermark by viewModel.applyWatermark.collectAsStateWithLifecycle()
    val isProSubscriber by viewModel.isProSubscriber.collectAsStateWithLifecycle()
    val slots by viewModel.slots.collectAsStateWithLifecycle()
    val activeSlotId by viewModel.effectiveActiveSlotId.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val tutorialEntryPoint =
        remember(context) {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                ExportSettingsTutorialEntryPoint::class.java,
            )
        }
    val tutorialCoordinator = remember(tutorialEntryPoint) { tutorialEntryPoint.tutorialCoordinator() }
    val onboardingStateRepository = remember(tutorialEntryPoint) { tutorialEntryPoint.onboardingStateRepository() }

    val listState = rememberLazyListState()
    val currentTutorialStep by tutorialCoordinator.currentStep.collectAsStateWithLifecycle()

    LaunchedEffect(tutorialCoordinator) {
        if (!onboardingStateRepository.isExportSettingsTutorialCompleted()) {
            tutorialCoordinator.start(TutorialSection.EXPORT_SETTINGS_INTRO)
        }
    }

    LaunchedEffect(currentTutorialStep) {
        val step = currentTutorialStep ?: return@LaunchedEffect
        if (step == TutorialStepId.EXPORT_PRESETS_INFO ||
            step == TutorialStepId.EXPORT_PRESET_DEFAULT_CARD_INFO
        ) {
            val lastIndex = listState.layoutInfo.totalItemsCount - 1
            if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
        }
    }

    val pairIdSet =
        remember(viewModel.pairIds) {
            viewModel.pairIds
                .split(',')
                .mapNotNull { it.trim().toLongOrNull() }
                .toSet()
        }

    var actionTarget by remember { mutableStateOf<ExportPresetSlot?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ExportPresetSlot?>(null) }

    val errorMessages =
        SlotErrorMessages(
            nameBlank = stringResource(R.string.export_preset_error_name_blank),
            nameTooLong = stringResource(R.string.export_preset_error_name_too_long),
            nameDuplicate = stringResource(R.string.export_preset_error_name_duplicate),
            limitReached = stringResource(R.string.export_preset_error_limit_reached),
            cannotDeleteDefault = stringResource(R.string.export_preset_error_cannot_delete_default),
            unknown = stringResource(R.string.export_preset_error_unknown),
        )

    LaunchedEffect(viewModel.slotError) {
        viewModel.slotError.collect { error ->
            val message =
                when (error) {
                    SlotError.NameBlank -> errorMessages.nameBlank
                    SlotError.NameTooLong -> errorMessages.nameTooLong
                    SlotError.NameDuplicate -> errorMessages.nameDuplicate
                    SlotError.LimitReached -> errorMessages.limitReached
                    SlotError.CannotDeleteDefault -> errorMessages.cannotDeleteDefault
                    SlotError.NotFound, SlotError.SelectFailed, SlotError.Unknown ->
                        errorMessages.unknown
                }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    ExportSettingsScreen(
        includeBefore = preset.includeBefore,
        includeAfter = preset.includeAfter,
        includeCombined = preset.includeCombined,
        format = preset.format,
        watermarkConfig = watermarkConfig,
        applyWatermark = applyWatermark,
        applyCombineConfig = preset.applyCombineConfig,
        isProSubscriber = isProSubscriber,
        slots = slots,
        activeSlotId = activeSlotId,
        onIncludeBeforeChange = viewModel::setIncludeBefore,
        onIncludeAfterChange = viewModel::setIncludeAfter,
        onIncludeCombinedChange = viewModel::setIncludeCombined,
        onFormatChange = viewModel::setFormat,
        onApplyWatermarkChange = viewModel::setApplyWatermark,
        onApplyCombineConfigChange = viewModel::setApplyCombineConfig,
        onNavigateBack = onNavigateBack,
        onNavigateToWatermarkSettings = onNavigateToWatermarkSettings,
        onNavigateToCombineSettings = onNavigateToCombineSettings,
        onProLock = { onNavigateToPaywall(PaywallTrigger.FEATURE_LOCKED) },
        onSelectSlot = viewModel::selectSlot,
        onLongPressSlot = { slot -> actionTarget = slot },
        onAddSlot = { showCreateDialog = true },
        onShare = { onShare(pairIdSet) },
        onSaveToDevice = { onSaveToDevice(pairIdSet) },
        onReplayTutorial = { tutorialCoordinator.start(TutorialSection.EXPORT_SETTINGS_INTRO) },
        listState = listState,
    )

    actionTarget?.let { slot ->
        PresetActionsDialog(
            slot = slot,
            onRename = {
                renameTarget = slot
                actionTarget = null
            },
            onDelete = {
                viewModel.deleteSlot(slot.id)
                actionTarget = null
            },
            onDismiss = { actionTarget = null },
        )
    }

    if (showCreateDialog) {
        PresetNameDialog(
            titleResId = R.string.export_preset_name_dialog_title_create,
            initialName = "",
            onConfirm = { name ->
                viewModel.createSlot(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    renameTarget?.let { slot ->
        PresetNameDialog(
            titleResId = R.string.export_preset_name_dialog_title_rename,
            initialName = slot.name,
            onConfirm = { name ->
                viewModel.renameSlot(slot.id, name)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }
}

private data class SlotErrorMessages(
    val nameBlank: String,
    val nameTooLong: String,
    val nameDuplicate: String,
    val limitReached: String,
    val cannotDeleteDefault: String,
    val unknown: String,
)
