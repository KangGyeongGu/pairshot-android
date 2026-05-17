package com.pairshot.core.rendering

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.pairshot.core.model.LensFacing
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Camera2SensorOrientation
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun degrees(facing: LensFacing): Int {
            val manager =
                context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                    ?: return DEFAULT_DEGREES
            val target =
                if (facing == LensFacing.BACK) {
                    CameraCharacteristics.LENS_FACING_BACK
                } else {
                    CameraCharacteristics.LENS_FACING_FRONT
                }
            return runCatching {
                val id =
                    manager.cameraIdList.firstOrNull { id ->
                        manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == target
                    } ?: return@runCatching DEFAULT_DEGREES
                manager
                    .getCameraCharacteristics(id)
                    .get(CameraCharacteristics.SENSOR_ORIENTATION) ?: DEFAULT_DEGREES
            }.getOrDefault(DEFAULT_DEGREES)
        }

        private companion object {
            const val DEFAULT_DEGREES = 90
        }
    }
