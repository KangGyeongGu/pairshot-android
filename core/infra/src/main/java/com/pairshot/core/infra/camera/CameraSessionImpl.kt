package com.pairshot.core.infra.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.pairshot.core.infra.sensor.SensorSession
import com.pairshot.core.model.AspectRatio
import com.pairshot.core.model.CameraCapabilities
import com.pairshot.core.model.FlashMode
import com.pairshot.core.model.LensFacing
import com.pairshot.core.model.ZoomRange
import com.pairshot.core.rendering.ExifBitmapLoader
import com.pairshot.core.rendering.OverlayTransformCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@ViewModelScoped
class CameraSessionImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val sensorSession: SensorSession,
        private val exifBitmapLoader: ExifBitmapLoader,
    ) : CameraSession {
        private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
        override val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest.asStateFlow()

        private val _capabilities = MutableStateFlow(CameraCapabilities())
        override val capabilities: StateFlow<CameraCapabilities> = _capabilities.asStateFlow()

        private val _zoomState = MutableStateFlow(ZoomRange(1f, 1f, 1f))
        override val zoomState: StateFlow<ZoomRange> = _zoomState.asStateFlow()

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        private var provider: ProcessCameraProvider? = null
        private var extensionsManager: ExtensionsManager? = null
        private var camera: Camera? = null
        private var owner: LifecycleOwner? = null

        private var imageCapture: ImageCapture = buildImageCapture(AspectRatio.RATIO_4_3)

        private val shutterSoundPlayer = ShutterSoundPlayer(context)

        private var lensFacing: LensFacing = LensFacing.BACK
        private var flashMode: FlashMode = FlashMode.OFF
        private var nightModeEnabled: Boolean = false
        private var hdrEnabled: Boolean = false
        private var exposureIndex: Int = 0
        private var aspectRatio: AspectRatio = AspectRatio.RATIO_4_3

        private var extensionsJob: Job? = null
        private var focusJob: Job? = null
        private var orientationJob: Job? = null

        override suspend fun bind(owner: LifecycleOwner) {
            this.owner = owner
            provider =
                provider ?: ProcessCameraProvider.awaitInstance(context).also { provider = it }
            extensionsManager =
                extensionsManager
                    ?: ExtensionsManager
                        .getInstanceAsync(context, provider!!)
                        .await()
                        .also { extensionsManager = it }
            rebindInternal()
            startOrientationObserver()
        }

        private fun startOrientationObserver() {
            if (orientationJob?.isActive == true) return
            orientationJob =
                scope.launch {
                    sensorSession.deviceOrientation.collect { rotation ->
                        imageCapture.targetRotation = rotation
                    }
                }
        }

        private fun scheduleRebind(debounceMs: Long) {
            extensionsJob?.cancel()
            extensionsJob =
                scope.launch {
                    if (debounceMs > 0) delay(debounceMs)
                    rebindInternal()
                }
        }

        private suspend fun rebindInternal() {
            val currentProvider = provider ?: return
            val currentOwner = owner ?: return
            val extManager = extensionsManager ?: return

            currentProvider.unbindAll()

            val baseSelector =
                if (lensFacing == LensFacing.BACK) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }

            val nightAvailable = extManager.isExtensionAvailable(baseSelector, ExtensionMode.NIGHT)
            val hdrAvailable = extManager.isExtensionAvailable(baseSelector, ExtensionMode.HDR)

            val cameraSelector =
                when {
                    nightModeEnabled && nightAvailable -> {
                        extManager.getExtensionEnabledCameraSelector(baseSelector, ExtensionMode.NIGHT)
                    }

                    hdrEnabled && hdrAvailable -> {
                        extManager.getExtensionEnabledCameraSelector(baseSelector, ExtensionMode.HDR)
                    }

                    else -> {
                        baseSelector
                    }
                }

            val previewResolutionSelector = buildResolutionSelector(aspectRatio)
            val preview =
                Preview
                    .Builder()
                    .setResolutionSelector(previewResolutionSelector)
                    .build()
            preview.setSurfaceProvider { request -> _surfaceRequest.value = request }

            applyFlashModeToImageCapture(flashMode)

            val newCamera =
                currentProvider.bindToLifecycle(
                    currentOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                )
            camera = newCamera

            updateCapabilitiesFromInfo(newCamera.cameraInfo, extManager, baseSelector)

            if (flashMode == FlashMode.TORCH) {
                newCamera.cameraControl.enableTorch(true)
            }

            if (exposureIndex != 0) {
                newCamera.cameraControl.setExposureCompensationIndex(exposureIndex)
            }

            newCamera.cameraInfo.zoomState.removeObservers(currentOwner)
            newCamera.cameraInfo.zoomState.observe(currentOwner) { zoom ->
                if (zoom != null) {
                    _zoomState.value =
                        ZoomRange(
                            min = zoom.minZoomRatio,
                            max = zoom.maxZoomRatio,
                            current = zoom.zoomRatio,
                        )
                }
            }
        }

        private fun updateCapabilitiesFromInfo(
            cameraInfo: CameraInfo,
            extManager: ExtensionsManager,
            baseSelector: CameraSelector,
        ) {
            val nightAvailable = extManager.isExtensionAvailable(baseSelector, ExtensionMode.NIGHT)
            val hdrAvailable = extManager.isExtensionAvailable(baseSelector, ExtensionMode.HDR)
            val exposureState = cameraInfo.exposureState
            val range = exposureState.exposureCompensationRange
            val step = exposureState.exposureCompensationStep

            _capabilities.value =
                CameraCapabilities(
                    hasFlash = cameraInfo.hasFlashUnit(),
                    nightModeAvailable = nightAvailable,
                    hdrAvailable = hdrAvailable,
                    exposureIndexMin = range.lower,
                    exposureIndexMax = range.upper,
                    exposureStepNumerator = step.numerator,
                    exposureStepDenominator = step.denominator,
                )
        }

        override suspend fun capture(): Result<String> {
            val capture = imageCapture
            val tempDir = File(context.cacheDir, "temp").also { it.mkdirs() }
            val tempFile = File(tempDir, "capture_${UUID.randomUUID()}.jpg")
            val options = ImageCapture.OutputFileOptions.Builder(tempFile).build()

            runCatching { camera?.cameraControl?.cancelFocusAndMetering() }

            return runCatching {
                shutterSoundPlayer.play()
                val savedUri: String =
                    suspendCancellableCoroutine { cont ->
                        capture.takePicture(
                            options,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onError(exception: ImageCaptureException) {
                                    scope.launch(NonCancellable) {
                                        withContext(Dispatchers.IO) {
                                            runCatching { tempFile.delete() }
                                        }
                                    }
                                    cont.resumeWithException(exception)
                                }

                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    val uri =
                                        outputFileResults.savedUri ?: Uri.fromFile(tempFile)
                                    if (aspectRatio == AspectRatio.RATIO_1_1) {
                                        runCatching { cropSquareInPlace(tempFile) }
                                            .onFailure { error ->
                                                Timber.w(error, "1:1 center crop failed; keeping 4:3 capture")
                                            }
                                    }
                                    cont.resume(uri.toString())
                                }
                            },
                        )
                        cont.invokeOnCancellation {
                            scope.launch(NonCancellable) {
                                withContext(Dispatchers.IO) {
                                    runCatching { tempFile.delete() }
                                }
                            }
                        }
                    }
                savedUri
            }.also {
                releaseMeteringAfterCapture()
            }
        }

        private fun releaseMeteringAfterCapture() {
            focusJob?.cancel()
            runCatching { camera?.cameraControl?.cancelFocusAndMetering() }
        }

        override fun setZoom(ratio: Float) {
            val zs = _zoomState.value
            val clamped = ratio.coerceIn(zs.min, zs.max)
            camera?.cameraControl?.setZoomRatio(clamped)
        }

        override fun setFlash(mode: FlashMode) {
            flashMode = mode
            applyFlashModeToImageCapture(mode)
            camera?.cameraControl?.enableTorch(mode == FlashMode.TORCH)
        }

        private fun applyFlashModeToImageCapture(mode: FlashMode) {
            imageCapture.flashMode =
                when (mode) {
                    FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
                    FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                    FlashMode.ON -> ImageCapture.FLASH_MODE_ON
                    FlashMode.TORCH -> ImageCapture.FLASH_MODE_OFF
                }
        }

        override fun setLensFacing(facing: LensFacing) {
            if (lensFacing == facing) return
            lensFacing = facing
            scheduleRebind(debounceMs = 0L)
        }

        override fun setNightMode(enabled: Boolean) {
            if (nightModeEnabled == enabled) return
            nightModeEnabled = enabled
            if (enabled) hdrEnabled = false
            scheduleRebind(debounceMs = EXTENSIONS_DEBOUNCE_MS)
        }

        override fun setHdrMode(enabled: Boolean) {
            if (hdrEnabled == enabled) return
            hdrEnabled = enabled
            if (enabled) nightModeEnabled = false
            scheduleRebind(debounceMs = EXTENSIONS_DEBOUNCE_MS)
        }

        override fun setAspectRatio(ratio: AspectRatio) {
            if (aspectRatio == ratio) return
            aspectRatio = ratio
            imageCapture = buildImageCapture(ratio)
            scheduleRebind(debounceMs = 0L)
        }

        override fun startFocusAndMetering(
            x: Float,
            y: Float,
            viewWidth: Float,
            viewHeight: Float,
        ) {
            focusJob?.cancel()
            focusJob =
                scope.launch {
                    delay(FOCUS_DEBOUNCE_MS)
                    val control: CameraControl = camera?.cameraControl ?: return@launch
                    val factory = SurfaceOrientedMeteringPointFactory(viewWidth, viewHeight)
                    val point = factory.createPoint(x, y)
                    val action =
                        FocusMeteringAction
                            .Builder(point)
                            .setAutoCancelDuration(FOCUS_AUTO_CANCEL_SECONDS, TimeUnit.SECONDS)
                            .build()
                    control.startFocusAndMetering(action)
                }
        }

        override fun setExposureIndex(index: Int) {
            exposureIndex = index
            camera?.cameraControl?.setExposureCompensationIndex(index)
        }

        override fun sensorRotationDegrees(facing: LensFacing): Int =
            camera?.cameraInfo?.sensorRotationDegrees
                ?: sensorRotationDegreesFromCamera2(facing)

        private fun sensorRotationDegreesFromCamera2(facing: LensFacing): Int {
            val cameraManager =
                context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                    ?: return DEFAULT_SENSOR_ORIENTATION_DEGREES
            val targetFacing =
                if (facing == LensFacing.BACK) {
                    CameraCharacteristics.LENS_FACING_BACK
                } else {
                    CameraCharacteristics.LENS_FACING_FRONT
                }
            return runCatching {
                val id =
                    cameraManager.cameraIdList.firstOrNull { id ->
                        val chars = cameraManager.getCameraCharacteristics(id)
                        chars.get(CameraCharacteristics.LENS_FACING) == targetFacing
                    } ?: return@runCatching DEFAULT_SENSOR_ORIENTATION_DEGREES
                cameraManager
                    .getCameraCharacteristics(id)
                    .get(CameraCharacteristics.SENSOR_ORIENTATION) ?: DEFAULT_SENSOR_ORIENTATION_DEGREES
            }.getOrDefault(DEFAULT_SENSOR_ORIENTATION_DEGREES)
        }

        override suspend fun readBeforeRotation(
            beforePhotoUri: String,
            lensFacing: LensFacing,
        ): Float =
            runCatching {
                withContext(Dispatchers.IO) {
                    val uri = Uri.parse(beforePhotoUri)
                    val exifDegrees = exifBitmapLoader.readExifDegrees(uri)
                    val sensor = sensorRotationDegrees(lensFacing)
                    OverlayTransformCalculator.calculate(sensor, exifDegrees)
                }
            }.onFailure { error ->
                Timber.w(error, "Before rotation read failed: $beforePhotoUri")
            }.getOrDefault(0f)

        override suspend fun prepareOverlay(
            beforePhotoUri: String,
            lensFacing: LensFacing,
        ): OverlayBitmap? =
            runCatching {
                withContext(Dispatchers.IO) {
                    val uri = Uri.parse(beforePhotoUri)
                    val source = exifBitmapLoader.loadBitmapWithExifCorrection(uri, inSampleSize = OVERLAY_IN_SAMPLE_SIZE)
                    val exifDegrees = exifBitmapLoader.readExifDegrees(uri)
                    val sensor = sensorRotationDegrees(lensFacing)
                    val rotation = OverlayTransformCalculator.calculate(sensor, exifDegrees)
                    if (rotation == 0f) {
                        OverlayBitmap(source, 0f)
                    } else {
                        val matrix = Matrix().apply { postRotate(rotation) }
                        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
                        if (rotated !== source) source.recycle()
                        OverlayBitmap(rotated, rotation)
                    }
                }
            }.onFailure { error ->
                Timber.w(error, "Overlay preparation failed: $beforePhotoUri")
            }.getOrNull()

        override fun playShutterSound() {
            shutterSoundPlayer.play()
        }

        override fun release() {
            extensionsJob?.cancel()
            focusJob?.cancel()
            orientationJob?.cancel()
            provider?.unbindAll()
            shutterSoundPlayer.release()
            camera = null
            _surfaceRequest.value = null
            scope.cancel()
        }

        private fun buildImageCapture(ratio: AspectRatio): ImageCapture =
            ImageCapture
                .Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setResolutionSelector(buildResolutionSelector(ratio))
                .build()

        private fun buildResolutionSelector(ratio: AspectRatio): ResolutionSelector {
            val strategy =
                when (ratio) {
                    AspectRatio.RATIO_4_3, AspectRatio.RATIO_1_1 ->
                        AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
                    AspectRatio.RATIO_16_9 ->
                        AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
                }
            return ResolutionSelector
                .Builder()
                .setAspectRatioStrategy(strategy)
                .build()
        }

        private fun cropSquareInPlace(file: File) {
            val source = BitmapFactory.decodeFile(file.absolutePath) ?: return
            val side = minOf(source.width, source.height)
            val offsetX = (source.width - side) / 2
            val offsetY = (source.height - side) / 2
            val cropped = Bitmap.createBitmap(source, offsetX, offsetY, side, side)
            try {
                FileOutputStream(file).use { out ->
                    cropped.compress(Bitmap.CompressFormat.JPEG, JPEG_CROP_QUALITY, out)
                }
            } finally {
                if (cropped !== source) cropped.recycle()
                source.recycle()
            }
        }

        companion object {
            private const val EXTENSIONS_DEBOUNCE_MS = 300L
            private const val FOCUS_DEBOUNCE_MS = 200L
            private const val OVERLAY_IN_SAMPLE_SIZE = 2
            private const val DEFAULT_SENSOR_ORIENTATION_DEGREES = 90
            private const val FOCUS_AUTO_CANCEL_SECONDS = 3L
            private const val JPEG_CROP_QUALITY = 95
        }
    }
