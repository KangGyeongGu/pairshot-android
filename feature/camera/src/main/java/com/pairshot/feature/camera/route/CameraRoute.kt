package com.pairshot.feature.camera.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.pairshot.core.designsystem.PairShotCameraTokens
import com.pairshot.core.navigation.PaywallTrigger
import com.pairshot.feature.camera.permission.CameraPermissionGate
import com.pairshot.feature.camera.screen.CameraScreen
import com.pairshot.feature.camera.viewmodel.CameraViewModel

@Composable
fun CameraRoute(
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: (PaywallTrigger) -> Unit,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(PairShotCameraTokens.Letterbox),
    ) {
        CameraPermissionGate(onNavigateBack = onNavigateBack) {
            CameraScreen(
                viewModel = viewModel,
                onNavigateBack = onNavigateBack,
                onNavigateToPaywall = onNavigateToPaywall,
            )
        }
    }
}
