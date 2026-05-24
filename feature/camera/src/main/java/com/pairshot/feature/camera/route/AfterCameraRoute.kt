package com.pairshot.feature.camera.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pairshot.core.designsystem.PairShotCameraTokens
import com.pairshot.feature.camera.permission.CameraPermissionGate
import com.pairshot.feature.camera.screen.AfterCameraScreen

@Composable
fun AfterCameraRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
        modifier
            .fillMaxSize()
            .background(PairShotCameraTokens.Letterbox),
    ) {
        CameraPermissionGate(onNavigateBack = onNavigateBack) {
            AfterCameraScreen(
                onNavigateBack = onNavigateBack,
            )
        }
    }
}
