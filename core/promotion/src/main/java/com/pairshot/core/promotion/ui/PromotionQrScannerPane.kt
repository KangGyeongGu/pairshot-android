package com.pairshot.core.promotion.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.pairshot.core.promotion.R
import timber.log.Timber
import java.util.concurrent.Executors

@Composable
fun PromotionQrScannerPane(
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted -> permissionGranted = granted }

    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (!permissionGranted) {
            Text(
                text = stringResource(R.string.promotion_qr_camera_permission_required),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            return@Box
        }
        QrScannerCameraPreview(
            onBarcode = { value ->
                if (value.isNotBlank()) onResult(value.trim())
            },
        )
    }
}

@Composable
private fun QrScannerCameraPreview(onBarcode: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner =
        remember {
            BarcodeScanning.getClient()
        }
    val previewView =
        remember {
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }
    var hasReported by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, previewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider =
                runCatching { providerFuture.get() }
                    .getOrElse {
                        Timber.w(it, "Promotion QR camera provider failed")
                        return@addListener
                    }
            val preview =
                androidx.camera.core.Preview
                    .Builder()
                    .build()
                    .apply { surfaceProvider = previewView.surfaceProvider }
            val analysis =
                ImageAnalysis
                    .Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

            analysis.setAnalyzer(analyzerExecutor) { proxy ->
                processImageProxy(proxy, barcodeScanner) { value ->
                    if (!hasReported) {
                        hasReported = true
                        onBarcode(value)
                    }
                }
            }
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            }.onFailure { Timber.w(it, "Promotion QR bind to lifecycle failed") }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            analyzerExecutor.shutdown()
            runCatching { barcodeScanner.close() }
            runCatching { providerFuture.get().unbindAll() }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize(),
    )
}

private fun processImageProxy(
    imageProxy: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onBarcode: (String) -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner
        .process(input)
        .addOnSuccessListener { barcodes ->
            val value =
                barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }?.rawValue
            if (!value.isNullOrBlank()) onBarcode(value)
        }.addOnFailureListener { Timber.w(it, "Promotion QR scan failure") }
        .addOnCompleteListener { imageProxy.close() }
}
