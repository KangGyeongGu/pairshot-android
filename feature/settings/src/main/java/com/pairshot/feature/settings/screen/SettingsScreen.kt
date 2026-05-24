package com.pairshot.feature.settings.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pairshot.core.adsui.component.PairShotBannerAd
import com.pairshot.core.designsystem.PairShotCard
import com.pairshot.core.designsystem.PairShotScreen
import com.pairshot.core.designsystem.PairShotSnackbarTokens
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.model.AppSettings
import com.pairshot.core.model.AppTextScale
import com.pairshot.core.model.AppTheme
import com.pairshot.core.model.ImageQualityPreset
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.navigation.SettingsHighlight
import com.pairshot.core.ui.component.PairShotSnackbarController
import com.pairshot.core.ui.component.PairShotSnackbarHost
import com.pairshot.core.ui.component.SettingsCard
import com.pairshot.core.ui.component.SettingsItem
import com.pairshot.core.ui.component.SettingsSectionLabel
import com.pairshot.feature.settings.R
import com.pairshot.feature.settings.component.HIGHLIGHT_PULSE_OFF_MS
import com.pairshot.feature.settings.component.HIGHLIGHT_PULSE_ON_MS
import com.pairshot.feature.settings.component.HighlightableSettingsCard
import com.pairshot.feature.settings.dialog.ClearCacheDialog
import com.pairshot.feature.settings.dialog.FileNamePrefixDialog
import com.pairshot.feature.settings.dialog.ImageQualityDialog
import com.pairshot.feature.settings.dialog.LanguageDialog
import com.pairshot.feature.settings.dialog.TextScaleDialog
import com.pairshot.feature.settings.dialog.ThemeDialog
import com.pairshot.feature.settings.locale.apply
import com.pairshot.feature.settings.locale.currentAppLocale
import com.pairshot.feature.settings.section.SettingsCaptureSection
import com.pairshot.feature.settings.section.SettingsGeneralSection
import com.pairshot.feature.settings.section.SettingsStorageInfoSection
import com.pairshot.feature.settings.section.SettingsWatermarkSection
import com.pairshot.feature.settings.viewmodel.SettingsUiState
import com.pairshot.feature.tutorial.ui.modifier.tutorialAnchor
import kotlinx.coroutines.delay
import com.pairshot.core.ui.R as CoreR

private const val HIGHLIGHT_WATERMARK_INDEX = 3
private const val HIGHLIGHT_COMBINE_INDEX = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    watermarkConfig: WatermarkConfig,
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    currentTextScale: AppTextScale,
    onTextScaleChange: (AppTextScale) -> Unit,
    onClearCache: () -> Unit,
    onLicenseClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onWatermarkConfigChange: (WatermarkConfig) -> Unit,
    onWatermarkSettingsClick: () -> Unit,
    onCombineSettingsClick: () -> Unit,
    onImageQualityChange: (ImageQualityPreset) -> Unit,
    onFileNamePrefixChange: (String) -> Unit,
    onOverlayEnabledChange: (Boolean) -> Unit,
    onOverlayAlphaChange: (Float) -> Unit,
    snackbarController: PairShotSnackbarController,
    showAdsConsent: Boolean,
    onAdsConsentClick: () -> Unit,
    onReplayTutorial: () -> Unit,
    highlight: SettingsHighlight? = null,
    proSubscriptionSection: @Composable () -> Unit = {},
) {
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showPrefixDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showTextScaleDialog by remember { mutableStateOf(false) }
    var currentLocale by remember { mutableStateOf(currentAppLocale()) }
    val listState = rememberLazyListState()
    var alreadyHighlighted by remember { mutableStateOf(false) }
    var highlightPulse by remember { mutableStateOf(false) }

    LaunchedEffect(highlight, uiState) {
        if (alreadyHighlighted) return@LaunchedEffect
        if (highlight == null) return@LaunchedEffect
        if (uiState !is SettingsUiState.Success) return@LaunchedEffect
        alreadyHighlighted = true
        val targetIndex =
            when (highlight) {
                SettingsHighlight.WATERMARK -> HIGHLIGHT_WATERMARK_INDEX
                SettingsHighlight.COMBINE -> HIGHLIGHT_COMBINE_INDEX
            }
        listState.animateScrollToItem(targetIndex)
        highlightPulse = true
        delay(HIGHLIGHT_PULSE_ON_MS)
        highlightPulse = false
        delay(HIGHLIGHT_PULSE_OFF_MS)
        highlightPulse = true
        delay(HIGHLIGHT_PULSE_ON_MS)
        highlightPulse = false
    }

    if (showLanguageDialog) {
        LanguageDialog(
            current = currentLocale,
            onSelect = { option ->
                currentLocale = option
                option.apply()
            },
            onDismiss = { showLanguageDialog = false },
        )
    }

    if (showThemeDialog) {
        ThemeDialog(
            current = currentTheme,
            onSelect = { option -> onThemeChange(option) },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showTextScaleDialog) {
        TextScaleDialog(
            current = currentTextScale,
            onSelect = onTextScaleChange,
            onDismiss = { showTextScaleDialog = false },
        )
    }

    val currentQuality = (uiState as? SettingsUiState.Success)?.imageQuality ?: ImageQualityPreset.DEFAULT
    val currentPrefix = (uiState as? SettingsUiState.Success)?.fileNamePrefix ?: AppSettings.DEFAULT_FILE_NAME_PREFIX
    val currentAlpha = (uiState as? SettingsUiState.Success)?.overlayAlpha ?: AppSettings.DEFAULT_OVERLAY_ALPHA

    if (showClearCacheDialog) {
        ClearCacheDialog(
            onConfirm = onClearCache,
            onDismiss = { showClearCacheDialog = false },
        )
    }

    if (showQualityDialog) {
        ImageQualityDialog(
            currentQuality = currentQuality,
            onQualityChange = onImageQualityChange,
            onDismiss = { showQualityDialog = false },
        )
    }

    if (showPrefixDialog) {
        FileNamePrefixDialog(
            currentPrefix = currentPrefix,
            onPrefixChange = onFileNamePrefixChange,
            onDismiss = { showPrefixDialog = false },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier =
            Modifier
                .fillMaxSize()
                .tutorialAnchor(com.pairshot.core.domain.tutorial.AnchorKey.SETTINGS_SCREEN),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(CoreR.string.common_title_settings),
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
            when (uiState) {
                is SettingsUiState.Loading -> {
                    Box(
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is SettingsUiState.Error -> {
                    Box(
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = uiState.message.asString(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is SettingsUiState.Success -> {
                    val qualityLabel =
                        when (uiState.imageQuality) {
                            ImageQualityPreset.LOW -> stringResource(R.string.settings_quality_low)
                            ImageQualityPreset.HIGH -> stringResource(R.string.settings_quality_high)
                            ImageQualityPreset.BEST -> stringResource(R.string.settings_quality_best)
                        }
                    val prefixDisplay =
                        if (uiState.fileNamePrefix.isEmpty()) {
                            stringResource(R.string.settings_file_name_prefix_none)
                        } else {
                            stringResource(R.string.settings_file_name_prefix_with_underscore, uiState.fileNamePrefix)
                        }

                    Column(
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        PairShotBannerAd()
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding =
                            PaddingValues(
                                horizontal = PairShotScreen.horizontalPadding,
                                vertical = PairShotCard.innerPadding,
                            ),
                        ) {
                            item(key = "section_pro_subscription") {
                                proSubscriptionSection()
                            }

                            item(key = "label_capture") {
                                SettingsSectionLabel(label = stringResource(R.string.settings_section_shooting_files))
                                Spacer(modifier = Modifier.height(PairShotSpacing.sm))
                            }

                            item(key = "card_capture") {
                                SettingsCaptureSection(
                                    qualityLabel = qualityLabel,
                                    onQualityClick = { showQualityDialog = true },
                                    overlayEnabled = uiState.overlayEnabled,
                                    overlayAlpha = currentAlpha,
                                    onOverlayEnabledChange = onOverlayEnabledChange,
                                    onOverlayAlphaChange = onOverlayAlphaChange,
                                    prefixDisplay = prefixDisplay,
                                    onPrefixClick = { showPrefixDialog = true },
                                )
                            }

                            item(key = "gap_capture") {
                                Spacer(modifier = Modifier.height(PairShotCard.innerPadding))
                            }

                            item(key = "label_watermark") {
                                SettingsSectionLabel(label = stringResource(R.string.settings_section_watermark))
                                Spacer(modifier = Modifier.height(PairShotSpacing.sm))
                            }

                            item(key = "card_watermark") {
                                SettingsWatermarkSection(
                                    watermarkConfig = watermarkConfig,
                                    pulse = highlightPulse && highlight == SettingsHighlight.WATERMARK,
                                    onWatermarkConfigChange = onWatermarkConfigChange,
                                    onWatermarkSettingsClick = onWatermarkSettingsClick,
                                )
                            }

                            item(key = "gap_watermark") {
                                Spacer(modifier = Modifier.height(PairShotCard.innerPadding))
                            }

                            item(key = "label_combine") {
                                SettingsSectionLabel(label = stringResource(R.string.settings_section_combine))
                                Spacer(modifier = Modifier.height(PairShotSpacing.sm))
                            }

                            item(key = "card_combine") {
                                HighlightableSettingsCard(
                                    pulse = highlightPulse && highlight == SettingsHighlight.COMBINE,
                                ) {
                                    SettingsItem(
                                        label = stringResource(R.string.settings_item_user_settings),
                                        onClick = onCombineSettingsClick,
                                    )
                                }
                            }

                            item(key = "gap_combine") {
                                Spacer(modifier = Modifier.height(PairShotCard.innerPadding))
                            }

                            item(key = "label_general") {
                                SettingsSectionLabel(label = stringResource(R.string.settings_section_general))
                                Spacer(modifier = Modifier.height(PairShotSpacing.sm))
                            }

                            item(key = "card_general") {
                                SettingsGeneralSection(
                                    currentLocale = currentLocale,
                                    currentTheme = currentTheme,
                                    currentTextScale = currentTextScale,
                                    showAdsConsent = showAdsConsent,
                                    onLanguageClick = { showLanguageDialog = true },
                                    onThemeClick = { showThemeDialog = true },
                                    onTextScaleClick = { showTextScaleDialog = true },
                                    onAdsConsentClick = onAdsConsentClick,
                                )
                            }

                            item(key = "gap_general") {
                                Spacer(modifier = Modifier.height(PairShotCard.innerPadding))
                            }

                            item(key = "label_help") {
                                SettingsSectionLabel(label = stringResource(R.string.settings_section_help))
                                Spacer(modifier = Modifier.height(PairShotSpacing.sm))
                            }

                            item(key = "card_help") {
                                SettingsCard {
                                    SettingsItem(
                                        label =
                                        stringResource(
                                            com.pairshot.feature.tutorial.R.string.tutorial_settings_replay_label,
                                        ),
                                        onClick = onReplayTutorial,
                                    )
                                }
                            }

                            item(key = "gap_help") {
                                Spacer(modifier = Modifier.height(PairShotCard.innerPadding))
                            }

                            item(key = "label_storage_info") {
                                SettingsSectionLabel(label = stringResource(R.string.settings_section_storage_and_info))
                                Spacer(modifier = Modifier.height(PairShotSpacing.sm))
                            }

                            item(key = "card_storage_info") {
                                SettingsStorageInfoSection(
                                    usedStorageBytes = uiState.usedStorageBytes,
                                    cacheBytes = uiState.cacheBytes,
                                    appVersion = uiState.appVersion,
                                    onClearCacheClick = { showClearCacheDialog = true },
                                    onLicenseClick = onLicenseClick,
                                    onPrivacyPolicyClick = onPrivacyPolicyClick,
                                )
                            }
                        }
                    }
                }
            }
        }

        PairShotSnackbarHost(
            controller = snackbarController,
            modifier =
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = PairShotSnackbarTokens.topOffset),
        )
    }
}
