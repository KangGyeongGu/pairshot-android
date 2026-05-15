package com.pairshot.feature.exportsettings.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pairshot.feature.exportsettings.screen.ExportSettingsScreen
import com.pairshot.feature.exportsettings.viewmodel.ExportSettingsViewModel

@Composable
fun ExportSettingsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToWatermarkSettings: () -> Unit,
    onNavigateToCombineSettings: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onShare: (Set<Long>) -> Unit,
    onSaveToDevice: (Set<Long>) -> Unit,
    viewModel: ExportSettingsViewModel = hiltViewModel(),
) {
    val preset by viewModel.preset.collectAsStateWithLifecycle()
    val watermarkConfig by viewModel.watermarkConfig.collectAsStateWithLifecycle()
    val applyWatermark by viewModel.applyWatermark.collectAsStateWithLifecycle()
    val isProSubscriber by viewModel.isProSubscriber.collectAsStateWithLifecycle()

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
        onProLocked = onNavigateToPaywall,
        onShare = { onShare(pairIdSet) },
        onSaveToDevice = { onSaveToDevice(pairIdSet) },
    )
}
