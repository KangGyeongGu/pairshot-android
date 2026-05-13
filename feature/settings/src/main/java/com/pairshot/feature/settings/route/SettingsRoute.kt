package com.pairshot.feature.settings.route

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.pairshot.core.ads.di.AdsEntryPoint
import com.pairshot.core.adsui.component.RewardedGateDialog
import com.pairshot.core.coupon.domain.CouponStatus
import com.pairshot.core.coupon.ui.CouponActivationUiState
import com.pairshot.core.coupon.ui.CouponRegisterDialog
import com.pairshot.core.coupon.ui.CouponStatusItem
import com.pairshot.core.coupon.ui.CouponViewModel
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.domain.premium.PremiumFeature
import com.pairshot.core.navigation.SettingsHighlight
import com.pairshot.core.ui.component.PairShotSnackbarController
import com.pairshot.core.ui.component.SettingsCard
import com.pairshot.core.ui.component.SettingsSectionLabel
import com.pairshot.core.ui.component.SnackbarEvent
import com.pairshot.core.ui.component.SnackbarVariant
import com.pairshot.core.ui.text.UiText
import com.pairshot.feature.settings.BuildConfig
import com.pairshot.feature.settings.screen.SettingsScreen
import com.pairshot.feature.settings.viewmodel.SettingsViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import com.pairshot.core.adsui.R as AdsUiR
import com.pairshot.core.coupon.R as CouponR

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToLicense: () -> Unit,
    onNavigateToWatermarkSettings: () -> Unit,
    onNavigateToCombineSettings: () -> Unit,
    highlight: SettingsHighlight? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
    couponViewModel: CouponViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val watermarkConfig by viewModel.watermarkConfig.collectAsStateWithLifecycle()
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val couponStatus by couponViewModel.status.collectAsStateWithLifecycle()
    val activationState by couponViewModel.activationState.collectAsStateWithLifecycle()
    val myCoupons by couponViewModel.myCoupons.collectAsStateWithLifecycle()
    val myCouponsLoading by couponViewModel.myCouponsLoading.collectAsStateWithLifecycle()
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
    val adFreeStatusProvider = remember(entryPoint) { entryPoint.adFreeStatusProvider() }
    val isAdFree by adFreeStatusProvider
        .observeIsAdFree()
        .collectAsStateWithLifecycle(initialValue = false)

    var showCouponDialog by remember { mutableStateOf(false) }
    var showRewardedGateDialog by remember { mutableStateOf<PremiumFeature?>(null) }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(activationState) {
        val state = activationState
        if (state is CouponActivationUiState.Success) {
            val days = state.durationDays
            val event =
                if (days == null) {
                    SnackbarEvent(
                        UiText.Resource(CouponR.string.coupon_success_registered_unlimited),
                        SnackbarVariant.SUCCESS,
                    )
                } else {
                    SnackbarEvent(
                        UiText.Resource(
                            CouponR.string.coupon_success_registered_days,
                            listOf(days.toInt()),
                        ),
                        SnackbarVariant.SUCCESS,
                    )
                }
            snackbarController.show(event)
        }
    }

    LaunchedEffect("snackbar") {
        viewModel.snackbarMessage.collect { event ->
            snackbarController.show(event)
        }
    }

    if (showCouponDialog) {
        CouponRegisterDialog(
            activationState = activationState,
            myCoupons = myCoupons,
            myCouponsLoading = myCouponsLoading,
            onActivate = { code -> couponViewModel.activate(code) },
            onLoadMyCoupons = { couponViewModel.loadMyCoupons() },
            onDismiss = {
                showCouponDialog = false
                couponViewModel.resetActivationState()
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
        highlight = highlight,
        onThemeChange = viewModel::updateAppTheme,
        onClearCache = viewModel::clearCache,
        onLicenseClick = onNavigateToLicense,
        onPrivacyPolicyClick = {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, BuildConfig.PRIVACY_POLICY_URL.toUri()),
                )
            }
        },
        onNavigateBack = onNavigateBack,
        onWatermarkConfigChange = viewModel::updateWatermarkConfig,
        onWatermarkSettingsClick = {
            if (isAdFree || settingsPremiumGate.isUnlocked(PremiumFeature.WATERMARK_DETAIL)) {
                onNavigateToWatermarkSettings()
            } else {
                showRewardedGateDialog = PremiumFeature.WATERMARK_DETAIL
            }
        },
        onCombineSettingsClick = {
            if (isAdFree || settingsPremiumGate.isUnlocked(PremiumFeature.COMBINE_DETAIL)) {
                onNavigateToCombineSettings()
            } else {
                showRewardedGateDialog = PremiumFeature.COMBINE_DETAIL
            }
        },
        onJpegQualityChange = viewModel::updateJpegQuality,
        onFileNamePrefixChange = viewModel::updateFileNamePrefix,
        onOverlayEnabledChange = viewModel::updateOverlayEnabled,
        onOverlayAlphaChange = viewModel::updateOverlayAlpha,
        snackbarController = snackbarController,
        couponSection = {
            CouponSection(
                status = couponStatus,
                onClick = {
                    couponViewModel.resetActivationState()
                    showCouponDialog = true
                },
            )
        },
    )
}

@Composable
private fun CouponSection(
    status: CouponStatus,
    onClick: () -> Unit,
) {
    SettingsSectionLabel(
        label = stringResource(CouponR.string.coupon_section_title),
    )
    Spacer(modifier = Modifier.height(PairShotSpacing.iconTextGap))
    SettingsCard {
        CouponStatusItem(
            status = status,
            nowMillis = System.currentTimeMillis(),
            onClick = onClick,
        )
    }
    Spacer(modifier = Modifier.height(PairShotSpacing.cardPadding))
}
