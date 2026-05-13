package com.pairshot.core.infra.camera

import androidx.camera.core.SurfaceRequest
import androidx.lifecycle.LifecycleOwner
import com.pairshot.core.model.AspectRatio
import com.pairshot.core.model.CameraCapabilities
import com.pairshot.core.model.FlashMode
import com.pairshot.core.model.LensFacing
import com.pairshot.core.model.ZoomRange
import kotlinx.coroutines.flow.StateFlow

interface CameraSession {
    val surfaceRequest: StateFlow<SurfaceRequest?>
    val capabilities: StateFlow<CameraCapabilities>
    val zoomState: StateFlow<ZoomRange>

    suspend fun bind(owner: LifecycleOwner)

    suspend fun capture(): Result<String>

    fun setZoom(ratio: Float)

    fun setFlash(mode: FlashMode)

    fun setLensFacing(facing: LensFacing)

    fun setNightMode(enabled: Boolean)

    fun setHdrMode(enabled: Boolean)

    fun setAspectRatio(ratio: AspectRatio)

    fun startFocusAndMetering(
        x: Float,
        y: Float,
        viewWidth: Float,
        viewHeight: Float,
    )

    fun setExposureIndex(index: Int)

    fun sensorRotationDegrees(facing: LensFacing): Int

    suspend fun prepareOverlay(
        beforePhotoUri: String,
        lensFacing: LensFacing,
    ): OverlayBitmap?

    suspend fun readBeforeRotation(
        beforePhotoUri: String,
        lensFacing: LensFacing,
    ): Float

    fun playShutterSound()

    fun release()
}
