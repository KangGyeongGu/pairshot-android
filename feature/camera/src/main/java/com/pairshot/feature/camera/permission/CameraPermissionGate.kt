package com.pairshot.feature.camera.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pairshot.core.designsystem.PairShotCameraTokens
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.feature.camera.R
import com.pairshot.core.ui.R as CoreR

@Composable
fun CameraPermissionGate(
    onNavigateBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED,
        )
    }
    var showRationale by remember { mutableStateOf(false) }
    var permissionPermanentlyDenied by remember { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                hasCameraPermission = true
            } else {
                val shouldShowRationale =
                    activity?.let {
                        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
                    } ?: false
                if (shouldShowRationale) {
                    showRationale = true
                } else {
                    permissionPermanentlyDenied = true
                }
            }
        }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            val shouldShowRationale =
                activity?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
                } ?: false
            if (shouldShowRationale) {
                showRationale = true
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    when {
        hasCameraPermission -> {
            content()
        }

        permissionPermanentlyDenied -> {
            PermissionDeniedContent(
                onOpenSettings = {
                    val intent =
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    context.startActivity(intent)
                },
                onNavigateBack = onNavigateBack,
            )
        }

        showRationale -> {
            PermissionRationaleContent(
                onRequestPermission = {
                    showRationale = false
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onNavigateBack = onNavigateBack,
            )
        }

        else -> {
        }
    }
}

@Composable
private fun PermissionRationaleContent(
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(PairShotCameraTokens.Letterbox)
                .padding(PairShotSpacing.xxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.camera_permission_title),
            color = PairShotCameraTokens.Foreground,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(PairShotSpacing.md))
        Text(
            text = stringResource(R.string.camera_permission_message),
            color = PairShotCameraTokens.Foreground.copy(alpha = 0.75f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(PairShotSpacing.xxl))
        Button(onClick = onRequestPermission) {
            Text(text = stringResource(R.string.camera_permission_allow))
        }
        Spacer(modifier = Modifier.height(PairShotSpacing.md))
        TextButton(onClick = onNavigateBack) {
            Text(
                text = stringResource(CoreR.string.common_button_cancel),
                color = PairShotCameraTokens.Foreground.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    onOpenSettings: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(PairShotCameraTokens.Letterbox)
                .padding(PairShotSpacing.xxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.camera_permission_denied_title),
            color = PairShotCameraTokens.Foreground,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(PairShotSpacing.md))
        Text(
            text = stringResource(R.string.camera_permission_denied_message),
            color = PairShotCameraTokens.Foreground.copy(alpha = 0.75f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(PairShotSpacing.xxl))
        Button(onClick = onOpenSettings) {
            Text(text = stringResource(R.string.camera_permission_go_settings))
        }
        Spacer(modifier = Modifier.height(PairShotSpacing.md))
        TextButton(onClick = onNavigateBack) {
            Text(
                text = stringResource(CoreR.string.common_button_cancel),
                color = PairShotCameraTokens.Foreground.copy(alpha = 0.6f),
            )
        }
    }
}
