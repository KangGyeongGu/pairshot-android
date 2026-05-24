package com.pairshot.feature.camera.component

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

data class ZoomUiState(
    val currentRatio: Float = 1f,
    val minRatio: Float = 1f,
    val maxRatio: Float = 1f,
    val presetRatios: List<Float> = listOf(1f),
    val customRatios: Map<Float, Float> = emptyMap(),
)

class ZoomStateHolder {
    private val _zoomUiState = MutableStateFlow(ZoomUiState())
    val zoomUiState: StateFlow<ZoomUiState> = _zoomUiState.asStateFlow()

    companion object {
        private const val MIN_ULTRA_WIDE_RATIO = 0.5f
        private const val TELE_PRESET_RATIO = 5f
        private const val CUSTOM_RATIO_SNAP_THRESHOLD = 0.05f
    }

    fun initFromZoomState(
        minRatio: Float,
        maxRatio: Float,
    ) {
        val current = _zoomUiState.value
        if (current.minRatio == minRatio && current.maxRatio == maxRatio) return
        val presets = buildPresetRatios(minRatio, maxRatio)
        _zoomUiState.update {
            it.copy(
                minRatio = minRatio,
                maxRatio = maxRatio,
                presetRatios = presets,
                customRatios = emptyMap(),
            )
        }
    }

    fun updateZoomRatio(ratio: Float) {
        val current = _zoomUiState.value
        val clamped = ratio.coerceIn(current.minRatio, current.maxRatio)
        _zoomUiState.update { it.copy(currentRatio = clamped) }
    }

    fun onPresetTap(preset: Float) {
        _zoomUiState.update {
            it.copy(
                currentRatio = preset,
                customRatios = it.customRatios - preset,
            )
        }
    }

    fun applyCustomRatio() {
        val state = _zoomUiState.value
        val nearest = state.presetRatios.minByOrNull { abs(it - state.currentRatio) } ?: return
        _zoomUiState.update {
            if (abs(state.currentRatio - nearest) > CUSTOM_RATIO_SNAP_THRESHOLD) {
                it.copy(customRatios = it.customRatios + (nearest to state.currentRatio))
            } else {
                it.copy(
                    currentRatio = nearest,
                    customRatios = it.customRatios - nearest,
                )
            }
        }
    }

    fun resetZoomForLensSwitch() {
        val current = _zoomUiState.value
        val resetRatio = 1f.coerceIn(current.minRatio, current.maxRatio)
        _zoomUiState.update {
            it.copy(
                currentRatio = resetRatio,
                customRatios = emptyMap(),
            )
        }
    }

    fun restoreZoomForPair(zoomLevel: Float?) {
        val ratio = zoomLevel ?: 1f
        val current = _zoomUiState.value
        val clamped = ratio.coerceIn(current.minRatio, current.maxRatio)
        _zoomUiState.update { it.copy(currentRatio = clamped) }
    }

    private fun buildPresetRatios(
        minRatio: Float,
        maxRatio: Float,
    ): List<Float> {
        val presets = mutableListOf<Float>()
        if (minRatio < 1f) presets.add(minRatio.coerceAtLeast(MIN_ULTRA_WIDE_RATIO))
        presets.add(1f)
        if (maxRatio >= 2f) presets.add(2f)
        if (maxRatio >= TELE_PRESET_RATIO) presets.add(TELE_PRESET_RATIO)
        return presets.distinct().sorted()
    }
}
