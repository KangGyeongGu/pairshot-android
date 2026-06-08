package com.pairshot.feature.settings.route

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.pairshot.core.ads.di.AdsEntryPoint
import com.pairshot.core.adsui.component.RewardedGateDialog
import com.pairshot.core.billing.domain.SubscriptionStatus
import com.pairshot.core.domain.premium.PremiumFeature
import com.pairshot.core.domain.tutorial.TutorialReplayController
import com.pairshot.core.navigation.SettingsHighlight
import com.pairshot.core.promotion.ui.PromotionRegisterDialog
import com.pairshot.core.promotion.ui.PromotionViewModel
import com.pairshot.core.ui.component.PairShotSnackbarController
import com.pairshot.core.ui.component.SnackbarEvent
import com.pairshot.core.ui.component.SnackbarVariant
import com.pairshot.core.ui.text.UiText
import com.pairshot.feature.settings.BuildConfig
import com.pairshot.feature.settings.R
import com.pairshot.feature.settings.component.ProSubscriptionSection
import com.pairshot.feature.settings.screen.SettingsScreen
import com.pairshot.feature.settings.viewmodel.SettingsViewModel
import com.pairshot.feature.settings.viewmodel.SubscriptionSettingsEvent
import com.pairshot.feature.settings.viewmodel.SubscriptionSettingsViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.pairshot.core.adsui.R as AdsUiR

private const val PRIVACY_PATH = "/privacy"

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToLicense: () -> Unit,
    onNavigateToWatermarkSettings: () -> Unit,
    onNavigateToCombineSettings: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    highlight: SettingsHighlight? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
    promotionViewModel: PromotionViewModel = hiltViewModel(),
    subscriptionViewModel: SubscriptionSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val watermarkConfig by viewModel.watermarkConfig.collectAsStateWithLifecycle()
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val appTextScale by viewModel.appTextScale.collectAsStateWithLifecycle()
    val activationState by promotionViewModel.activationState.collectAsStateWithLifecycle()
    val myPromotions by promotionViewModel.myPromotions.collectAsStateWithLifecycle()
    val myPromotionsLoading by promotionViewModel.myPromotionsLoading.collectAsStateWithLifecycle()
    val subscriptionState by subscriptionViewModel.state.collectAsStateWithLifecycle()
    val snackbarController = remember { PairShotSnackbarController() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val activity = LocalActivity.current
    val snackbarScope = rememberCoroutineScope()

    val entryPoint =
        remember(context) {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                AdsEntryPoint::class.java,
            )
        }
    val rewardedAdController = remember(entryPoint) { entryPoint.rewardedAdController() }
    val settingsPremiumGate = remember(entryPoint) { entryPoint.settingsPremiumGate() }
    val membershipProvider = remember(entryPoint) { entryPoint.membershipProvider() }
    val adsInitializer = remember(entryPoint) { entryPoint.adsInitializer() }
    val tutorialEntryPoint =
        remember(context) {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                SettingsTutorialEntryPoint::class.java,
            )
        }
    val tutorialReplay: TutorialReplayController =
        remember(tutorialEntryPoint) { tutorialEntryPoint.tutorialReplayController() }
    val isProFlow = remember(membershipProvider) { membershipProvider.observe().map { it.isPro } }
    val isPro by isProFlow.collectAsStateWithLifecycle(initialValue = false)
    val showAdsConsent by adsInitializer.privacyOptionsRequired.collectAsStateWithLifecycle()

    var showPromotionDialog by remember { mutableStateOf(false) }
    var showRewardedGateDialog by remember { mutableStateOf<PremiumFeature?>(null) }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    LaunchedEffect("snackbar") {
        viewModel.snackbarMessage.collect { event ->
            snackbarController.show(event)
        }
    }

    LaunchedEffect("subscription-events") {
        subscriptionViewModel.events.collect { event ->
            val resId =
                when (event) {
                    SubscriptionSettingsEvent.RestoreSuccess -> R.string.settings_pro_restore_success
                    SubscriptionSettingsEvent.RestoreEmpty -> R.string.settings_pro_restore_empty
                }
            val variant =
                when (event) {
                    SubscriptionSettingsEvent.RestoreSuccess -> SnackbarVariant.SUCCESS
                    SubscriptionSettingsEvent.RestoreEmpty -> SnackbarVariant.WARNING
                }
            snackbarController.show(SnackbarEvent(UiText.Resource(resId), variant))
        }
    }

    if (showPromotionDialog) {
        PromotionRegisterDialog(
            activationState = activationState,
            myPromotions = myPromotions.toImmutableList(),
            myPromotionsLoading = myPromotionsLoading,
            onActivate = { code -> promotionViewModel.activate(code) },
            onLoadMyPromotions = { promotionViewModel.loadMyPromotions() },
            onDismiss = {
                showPromotionDialog = false
                promotionViewModel.resetActivationState()
            },
        )
    }

    val pendingFeature = showRewardedGateDialog
    if (pendingFeature != null) {
        RewardedGateDialog(
            feature = pendingFeature,
            onConfirm = {
                val act = activity ?: return@RewardedGateDialog
                showRewardedGateDialog = null
                rewardedAdController.showIfAvailable(
                    activity = act,
                    feature = pendingFeature,
                    onReward = {
                        when (pendingFeature) {
                            PremiumFeature.WATERMARK_DETAIL -> onNavigateToWatermarkSettings()
                            PremiumFeature.COMBINE_DETAIL -> onNavigateToCombineSettings()
                            PremiumFeature.EXPORT_PRESET -> Unit
                        }
                    },
                    onSkip = {
                        snackbarScope.launch {
                            snackbarController.show(
                                SnackbarEvent(
                                    UiText.Resource(AdsUiR.string.rewarded_gate_load_failed),
                                    SnackbarVariant.WARNING,
                                ),
                            )
                        }
                    },
                )
            },
            onDismiss = { showRewardedGateDialog = null },
        )
    }

    SettingsScreen(
        uiState = uiState,
        watermarkConfig = watermarkConfig,
        currentTheme = appTheme,
        currentTextScale = appTextScale,
        highlight = highlight,
        onThemeChange = viewModel::updateAppTheme,
        onTextScaleChange = viewModel::updateAppTextScale,
        onClearCache = viewModel::clearCache,
        onLicenseClick = onNavigateToLicense,
        onPrivacyPolicyClick = {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, (BuildConfig.WEB_BASE_URL.trimEnd('/') + PRIVACY_PATH).toUri()),
                )
            }
        },
        onNavigateBack = onNavigateBack,
        onWatermarkConfigChange = viewModel::updateWatermarkConfig,
        onWatermarkSettingsClick = {
            if (isPro || settingsPremiumGate.isUnlocked(PremiumFeature.WATERMARK_DETAIL)) {
                onNavigateToWatermarkSettings()
            } else {
                showRewardedGateDialog = PremiumFeature.WATERMARK_DETAIL
            }
        },
        onCombineSettingsClick = {
            if (isPro || settingsPremiumGate.isUnlocked(PremiumFeature.COMBINE_DETAIL)) {
                onNavigateToCombineSettings()
            } else {
                showRewardedGateDialog = PremiumFeature.COMBINE_DETAIL
            }
        },
        onImageQualityChange = viewModel::updateImageQuality,
        onFileNamePrefixChange = viewModel::updateFileNamePrefix,
        onOverlayEnabledChange = viewModel::updateOverlayEnabled,
        onOverlayAlphaChange = viewModel::updateOverlayAlpha,
        snackbarController = snackbarController,
        showAdsConsent = showAdsConsent,
        onAdsConsentClick = {
            activity?.let { act ->
                adsInitializer.showPrivacyOptionsForm(act) { error ->
                    if (error != null) {
                        snackbarScope.launch {
                            snackbarController.show(
                                SnackbarEvent(
                                    UiText.Resource(R.string.settings_pro_manage_failed),
                                    SnackbarVariant.ERROR,
                                ),
                            )
                        }
                    }
                }
            }
        },
        onReplayTutorial = {
            onNavigateBack()
            tutorialReplay.restart()
        },
        proSubscriptionSection = {
            ProSubscriptionSection(
                membership = subscriptionState.membership,
                subscriptionStatus = subscriptionState.subscriptionStatus,
                onLearnMore = onNavigateToPaywall,
                onViewSubscriptionOptions = onNavigateToPaywall,
                onManageSubscription = {
                    val productId = (subscriptionState.subscriptionStatus as? SubscriptionStatus.Active)?.productId
                    runCatching {
                        context.startActivity(subscriptionViewModel.manageSubscriptionsIntent(productId))
                    }.onFailure {
                        snackbarScope.launch {
                            snackbarController.show(
                                SnackbarEvent(
                                    UiText.Resource(R.string.settings_pro_manage_failed),
                                    SnackbarVariant.ERROR,
                                ),
                            )
                        }
                    }
                },
                onRestore = { subscriptionViewModel.restore() },
                onPromoCode = {
                    promotionViewModel.loadMyPromotions()
                    showPromotionDialog = true
                },
            )
        },
    )
}
