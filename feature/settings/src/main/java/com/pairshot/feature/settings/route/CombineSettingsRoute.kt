package com.pairshot.feature.settings.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pairshot.core.navigation.PaywallTrigger
import com.pairshot.feature.settings.screen.CombineSettingsScreen
import com.pairshot.feature.settings.viewmodel.CombineSettingsViewModel

@Composable
fun CombineSettingsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: (PaywallTrigger) -> Unit,
    viewModel: CombineSettingsViewModel = hiltViewModel(),
) {
    val combineConfig by viewModel.combineConfig.collectAsStateWithLifecycle()
    val watermarkConfig by viewModel.watermarkConfig.collectAsStateWithLifecycle()
    val isProSubscriber by viewModel.isProSubscriber.collectAsStateWithLifecycle()
    CombineSettingsScreen(
        combineConfig = combineConfig,
        watermarkConfig = watermarkConfig,
        isProSubscriber = isProSubscriber,
        onCombineConfigChange = viewModel::updateCombineConfig,
        onNavigateBack = onNavigateBack,
        onProLocked = { onNavigateToPaywall(PaywallTrigger.FEATURE_LOCKED) },
    )
}
