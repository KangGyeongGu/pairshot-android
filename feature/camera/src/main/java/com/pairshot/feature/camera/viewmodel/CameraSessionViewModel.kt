package com.pairshot.feature.camera.viewmodel

import androidx.lifecycle.ViewModel
import com.pairshot.core.infra.camera.CameraSession
import com.pairshot.core.infra.sensor.SensorSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CameraSessionViewModel
@Inject
constructor(
    val cameraSession: CameraSession,
    val sensorSession: SensorSession,
) : ViewModel() {
    override fun onCleared() {
        cameraSession.release()
        sensorSession.release()
    }
}
