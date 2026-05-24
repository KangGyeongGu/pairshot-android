package com.pairshot.feature.camera.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.pairshot.core.designsystem.PairShotCameraTokens
import com.pairshot.feature.camera.permission.CameraPermissionGate
import com.pairshot.feature.camera.screen.AfterCameraScreen
import com.pairshot.feature.camera.viewmodel.AfterCameraViewModel

@Composable
fun AfterCameraRoute(
    onNavigateBack: () -> Unit,
    viewModel: AfterCameraViewModel = hiltViewModel(),
) {
    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(PairShotCameraTokens.Letterbox),
    ) {
        CameraPermissionGate(onNavigateBack = onNavigateBack) {
            AfterCameraScreen(
                viewModel = viewModel,
                onNavigateBack = onNavigateBack,
            )
        }
    }
}
