package com.pairshot.feature.settings.route

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pairshot.core.navigation.PaywallTrigger
import com.pairshot.core.rendering.PreviewSampleProvider
import com.pairshot.core.rendering.WatermarkRenderer
import com.pairshot.feature.settings.screen.WatermarkSettingsScreen
import com.pairshot.feature.settings.viewmodel.SettingsViewModel
import dagger.hilt.android.EntryPointAccessors

@Composable
fun WatermarkSettingsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: (PaywallTrigger) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val watermarkConfig by viewModel.watermarkConfig.collectAsStateWithLifecycle()
    val isProSubscriber by viewModel.isProSubscriber.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val entryPoint =
        remember(context) {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                WatermarkSettingsRenderEntryPoint::class.java,
            )
        }
    val watermarkRenderer = remember(entryPoint) { entryPoint.watermarkRenderer() }
    val previewSampleProvider = remember(entryPoint) { entryPoint.previewSampleProvider() }

    val logoPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            uri?.let { viewModel.saveLogoFile(it.toString()) }
        }

    WatermarkSettingsScreen(
        watermarkConfig = watermarkConfig,
        isProSubscriber = isProSubscriber,
        onWatermarkConfigChange = viewModel::updateWatermarkConfig,
        onSelectLogo = { logoPickerLauncher.launch(arrayOf("image/*")) },
        onRemoveLogo = viewModel::removeLogo,
        onNavigateBack = onNavigateBack,
        onProLocked = { onNavigateToPaywall(PaywallTrigger.FEATURE_LOCKED) },
        watermarkRenderer = watermarkRenderer,
        previewSampleProvider = previewSampleProvider,
    )
}
