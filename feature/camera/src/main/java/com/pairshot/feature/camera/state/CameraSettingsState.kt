package com.pairshot.feature.camera.state

import com.pairshot.core.model.AspectRatio
import com.pairshot.core.model.FlashMode

data class CameraSettingsState(
    val flashMode: FlashMode = FlashMode.OFF,
    val gridEnabled: Boolean = false,
    val levelEnabled: Boolean = false,
    val nightModeEnabled: Boolean = false,
    val hdrEnabled: Boolean = false,
    val exposureIndex: Int = 0,
    val showPanel: Boolean = false,
    val aspectRatio: AspectRatio = AspectRatio.RATIO_4_3,
    val aspectRatioLocked: Boolean = false,
)
