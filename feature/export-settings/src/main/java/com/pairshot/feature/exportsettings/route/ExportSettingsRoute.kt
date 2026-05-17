package com.pairshot.feature.exportsettings.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pairshot.core.navigation.PaywallTrigger
import com.pairshot.feature.exportsettings.screen.ExportSettingsScreen
import com.pairshot.feature.exportsettings.viewmodel.ExportSettingsViewModel
import com.pairshot.feature.tutorial.domain.TutorialSection
import dagger.hilt.android.EntryPointAccessors

@Composable
fun ExportSettingsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToWatermarkSettings: () -> Unit,
    onNavigateToCombineSettings: () -> Unit,
    onNavigateToPaywall: (PaywallTrigger) -> Unit,
    onShare: (Set<Long>) -> Unit,
    onSaveToDevice: (Set<Long>) -> Unit,
    viewModel: ExportSettingsViewModel = hiltViewModel(),
) {
    val preset by viewModel.preset.collectAsStateWithLifecycle()
    val watermarkConfig by viewModel.watermarkConfig.collectAsStateWithLifecycle()
    val applyWatermark by viewModel.applyWatermark.collectAsStateWithLifecycle()
    val isProSubscriber by viewModel.isProSubscriber.collectAsStateWithLifecycle()

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

    LaunchedEffect(tutorialCoordinator) {
        if (!onboardingStateRepository.isExportSettingsTutorialCompleted()) {
            tutorialCoordinator.start(TutorialSection.EXPORT_SETTINGS_INTRO)
        }
    }

    val pairIdSet =
        remember(viewModel.pairIds) {
            viewModel.pairIds
                .split(',')
                .mapNotNull { it.trim().toLongOrNull() }
                .toSet()
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
        onIncludeBeforeChange = viewModel::setIncludeBefore,
        onIncludeAfterChange = viewModel::setIncludeAfter,
        onIncludeCombinedChange = viewModel::setIncludeCombined,
        onFormatChange = viewModel::setFormat,
        onApplyWatermarkChange = viewModel::setApplyWatermark,
        onApplyCombineConfigChange = viewModel::setApplyCombineConfig,
        onNavigateBack = onNavigateBack,
        onNavigateToWatermarkSettings = onNavigateToWatermarkSettings,
        onNavigateToCombineSettings = onNavigateToCombineSettings,
        onProLocked = { onNavigateToPaywall(PaywallTrigger.FEATURE_LOCKED) },
        onShare = { onShare(pairIdSet) },
        onSaveToDevice = { onSaveToDevice(pairIdSet) },
    )
}
