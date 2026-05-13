package com.pairshot.feature.camera.state

import com.pairshot.core.model.AspectRatio
import com.pairshot.core.model.FlashMode

data class InitialCameraSessionConfig(
    val flashMode: FlashMode,
    val nightModeEnabled: Boolean,
    val hdrEnabled: Boolean,
    val aspectRatio: AspectRatio,
)

data class CapabilityAdjustment(
    val flashMode: FlashMode? = null,
    val nightModeEnabled: Boolean? = null,
    val hdrEnabled: Boolean? = null,
)
