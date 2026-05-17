package com.pairshot.feature.settings.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pairshot.core.adsui.component.PairShotBannerAd
import com.pairshot.core.designsystem.PairShotCard
import com.pairshot.core.designsystem.PairShotScreen
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.model.WatermarkType
import com.pairshot.core.model.isContentMissing
import com.pairshot.core.rendering.PreviewSampleProvider
import com.pairshot.core.rendering.WatermarkRenderer
import com.pairshot.core.ui.component.SettingsCard
import com.pairshot.core.ui.component.SettingsDivider
import com.pairshot.core.ui.component.SettingsSectionLabel
import com.pairshot.core.ui.component.SettingsSwitchItem
import com.pairshot.feature.settings.R
import com.pairshot.feature.settings.component.WatermarkLogoSection
import com.pairshot.feature.settings.component.WatermarkPreviewSection
import com.pairshot.feature.settings.component.WatermarkTextSection
import com.pairshot.feature.settings.component.WatermarkTypeItem
import com.pairshot.core.ui.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkSettingsScreen(
    watermarkConfig: WatermarkConfig,
    isProSubscriber: Boolean,
    onWatermarkConfigChange: (WatermarkConfig) -> Unit,
    onSelectLogo: () -> Unit,
    onRemoveLogo: () -> Unit,
    onNavigateBack: () -> Unit,
    onProLocked: () -> Unit,
    watermarkRenderer: WatermarkRenderer,
    previewSampleProvider: PreviewSampleProvider,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.watermark_settings_title),
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
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            PairShotBannerAd()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        horizontal = PairShotScreen.horizontalPadding,
                        vertical = PairShotCard.innerPadding,
                    ),
            ) {
                item(key = "label_basic") {
                    SettingsSectionLabel(label = stringResource(R.string.watermark_section_basic))
                    Spacer(modifier = Modifier.height(PairShotSpacing.sm))
                }

                item(key = "card_basic") {
                    SettingsCard {
                        SettingsSwitchItem(
                            label = stringResource(R.string.settings_item_watermark_use),
                            checked = watermarkConfig.enabled,
                            onCheckedChange = { checked ->
                                onWatermarkConfigChange(watermarkConfig.copy(enabled = checked))
                            },
                        )
                        SettingsDivider()
                        WatermarkTypeItem(
                            selectedType = watermarkConfig.type,
                            isProSubscriber = isProSubscriber,
                            onTypeChange = { type ->
                                onWatermarkConfigChange(watermarkConfig.copy(type = type))
                            },
                            onProLocked = onProLocked,
                        )
                    }
                }

                val showWarning = watermarkConfig.enabled && watermarkConfig.isContentMissing()

                if (watermarkConfig.type == WatermarkType.TEXT) {
                    item(key = "label_text") {
                        Spacer(modifier = Modifier.height(PairShotSpacing.xxl))
                        SettingsSectionLabel(
                            label = stringResource(R.string.watermark_item_text_settings),
                            trailingWarning = if (showWarning) stringResource(R.string.settings_warning_required) else null,
                        )
                        Spacer(modifier = Modifier.height(PairShotSpacing.sm))
                    }
                    item(key = "card_text") {
                        WatermarkTextSection(
                            watermarkConfig = watermarkConfig,
                            onWatermarkConfigChange = onWatermarkConfigChange,
                        )
                    }
                }

                if (watermarkConfig.type == WatermarkType.LOGO) {
                    item(key = "label_logo") {
                        Spacer(modifier = Modifier.height(PairShotSpacing.xxl))
                        SettingsSectionLabel(
                            label = stringResource(R.string.watermark_item_logo_settings),
                            trailingWarning = if (showWarning) stringResource(R.string.settings_warning_required) else null,
                        )
                        Spacer(modifier = Modifier.height(PairShotSpacing.sm))
                    }
                    item(key = "card_logo") {
                        WatermarkLogoSection(
                            watermarkConfig = watermarkConfig,
                            onWatermarkConfigChange = onWatermarkConfigChange,
                            onSelectLogo = onSelectLogo,
                            onRemoveLogo = onRemoveLogo,
                        )
                    }
                }

                item(key = "wm_preview") {
                    Spacer(modifier = Modifier.height(PairShotSpacing.xxl))
                    SettingsSectionLabel(label = stringResource(R.string.watermark_section_preview))
                    Spacer(modifier = Modifier.height(PairShotSpacing.sm))
                    WatermarkPreviewSection(
                        config = watermarkConfig,
                        watermarkRenderer = watermarkRenderer,
                        previewSampleProvider = previewSampleProvider,
                    )
                    Spacer(modifier = Modifier.height(PairShotSpacing.xxl))
                }
            }
        }
    }
}
