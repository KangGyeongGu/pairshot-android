package com.pairshot.feature.exportsettings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pairshot.core.adsui.component.PairShotBannerAd
import com.pairshot.core.designsystem.PairShotButton
import com.pairshot.core.designsystem.PairShotCard
import com.pairshot.core.designsystem.PairShotScreen
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.model.ExportFormat
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.model.isContentMissing
import com.pairshot.core.ui.component.SettingsSectionLabel
import com.pairshot.feature.exportsettings.R
import com.pairshot.feature.exportsettings.component.ExportCombineSection
import com.pairshot.feature.exportsettings.component.ExportFormatSection
import com.pairshot.feature.exportsettings.component.ExportIncludeSection
import com.pairshot.feature.exportsettings.component.ExportWatermarkSection
import com.pairshot.feature.tutorial.ui.modifier.tutorialAnchor
import com.pairshot.core.ui.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun ExportSettingsScreen(
    includeBefore: Boolean,
    includeAfter: Boolean,
    includeCombined: Boolean,
    format: ExportFormat,
    watermarkConfig: WatermarkConfig,
    applyWatermark: Boolean,
    applyCombineConfig: Boolean,
    isProSubscriber: Boolean,
    onIncludeBeforeChange: (Boolean) -> Unit,
    onIncludeAfterChange: (Boolean) -> Unit,
    onIncludeCombinedChange: (Boolean) -> Unit,
    onFormatChange: (ExportFormat) -> Unit,
    onApplyWatermarkChange: (Boolean) -> Unit,
    onApplyCombineConfigChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToWatermarkSettings: () -> Unit,
    onNavigateToCombineSettings: () -> Unit,
    onProLocked: () -> Unit,
    onShare: () -> Unit,
    onSaveToDevice: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.export_settings_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(CoreR.string.common_desc_back),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                PairShotBannerAd()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = PairShotScreen.horizontalPadding,
                            end = PairShotScreen.horizontalPadding,
                            top = PairShotCard.innerPadding,
                            bottom = ACTION_BAR_RESERVED_HEIGHT,
                        ),
                ) {
                    item(key = "label_include") {
                        SettingsSectionLabel(label = stringResource(R.string.export_section_include))
                        Spacer(modifier = Modifier.height(PairShotSpacing.sm))
                    }
                    item(key = "section_include") {
                        Box(modifier = Modifier.tutorialAnchor(com.pairshot.core.domain.tutorial.AnchorKey.EXPORT_SECTION_INCLUDE)) {
                            ExportIncludeSection(
                                includeBefore = includeBefore,
                                includeAfter = includeAfter,
                                includeCombined = includeCombined,
                                onIncludeBeforeChange = onIncludeBeforeChange,
                                onIncludeAfterChange = onIncludeAfterChange,
                                onIncludeCombinedChange = onIncludeCombinedChange,
                            )
                        }
                    }

                    item(key = "label_format") {
                        Spacer(modifier = Modifier.height(PairShotSpacing.xxl))
                        SettingsSectionLabel(label = stringResource(R.string.export_section_format))
                        Spacer(modifier = Modifier.height(PairShotSpacing.sm))
                    }
                    item(key = "section_format") {
                        Box(modifier = Modifier.tutorialAnchor(com.pairshot.core.domain.tutorial.AnchorKey.EXPORT_SECTION_FORMAT)) {
                            ExportFormatSection(
                                format = format,
                                isProSubscriber = isProSubscriber,
                                onFormatChange = onFormatChange,
                                onProLocked = onProLocked,
                            )
                        }
                    }

                    val watermarkWarning =
                        applyWatermark && watermarkConfig.isContentMissing()

                    item(key = "label_watermark") {
                        Spacer(modifier = Modifier.height(PairShotSpacing.xxl))
                        SettingsSectionLabel(
                            label = stringResource(R.string.export_section_watermark),
                            trailingWarning =
                                if (watermarkWarning) {
                                    stringResource(R.string.export_warning_required)
                                } else {
                                    null
                                },
                        )
                        Spacer(modifier = Modifier.height(PairShotSpacing.sm))
                    }
                    item(key = "section_watermark") {
                        Box(modifier = Modifier.tutorialAnchor(com.pairshot.core.domain.tutorial.AnchorKey.EXPORT_SECTION_WATERMARK)) {
                            ExportWatermarkSection(
                                applyWatermark = applyWatermark,
                                onApplyWatermarkChange = onApplyWatermarkChange,
                                onNavigateToWatermarkSettings = onNavigateToWatermarkSettings,
                            )
                        }
                    }

                    item(key = "label_combine") {
                        Spacer(modifier = Modifier.height(PairShotSpacing.xxl))
                        SettingsSectionLabel(label = stringResource(R.string.export_section_combine))
                        Spacer(modifier = Modifier.height(PairShotSpacing.sm))
                    }
                    item(key = "section_combine") {
                        Box(modifier = Modifier.tutorialAnchor(com.pairshot.core.domain.tutorial.AnchorKey.EXPORT_SECTION_COMBINE)) {
                            ExportCombineSection(
                                applyCombineConfig = applyCombineConfig,
                                onApplyCombineConfigChange = onApplyCombineConfigChange,
                                onNavigateToCombineSettings = onNavigateToCombineSettings,
                            )
                        }
                    }

                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(PairShotSpacing.xxl))
                    }
                }
            }

            ActionBar(
                onShare = onShare,
                onSaveToDevice = onSaveToDevice,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun ActionBar(
    onShare: () -> Unit,
    onSaveToDevice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .navigationBarsPadding()
                .padding(horizontal = ACTION_BAR_HORIZONTAL_PADDING, vertical = ACTION_BAR_VERTICAL_PADDING),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        FloatingActionButton(
            onClick = onShare,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            elevation =
                FloatingActionButtonDefaults.elevation(
                    defaultElevation = FAB_ELEVATION,
                ),
            modifier = Modifier.size(FAB_SIZE),
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = stringResource(CoreR.string.common_button_share),
            )
        }
        FloatingActionButton(
            onClick = onSaveToDevice,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation =
                FloatingActionButtonDefaults.elevation(
                    defaultElevation = FAB_ELEVATION,
                ),
            modifier = Modifier.size(FAB_SIZE),
        ) {
            Icon(
                imageVector = Icons.Outlined.FileDownload,
                contentDescription = stringResource(CoreR.string.common_button_save_to_device),
            )
        }
    }
}

private val FAB_SIZE = PairShotButton.fabSize
private val FAB_ELEVATION = PairShotSpacing.xs
private val ACTION_BAR_HORIZONTAL_PADDING = PairShotSpacing.xl
private val ACTION_BAR_VERTICAL_PADDING = PairShotSpacing.lg
private val ACTION_BAR_RESERVED_HEIGHT = PairShotSpacing.xxxl + PairShotSpacing.xxxl
