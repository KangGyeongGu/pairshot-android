package com.pairshot.core.designsystem

import android.util.DisplayMetrics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.pairshot.core.model.AppTextScale

val LocalAppTextScale =
    compositionLocalOf { AppTextScale.DEFAULT }

@Composable
fun ProvideAppTextScaleDensity(content: @Composable () -> Unit) {
    val textScale = LocalAppTextScale.current
    val fixed =
        remember(textScale) {
            val stableDensityValue =
                DisplayMetrics.DENSITY_DEVICE_STABLE.toFloat() / DisplayMetrics.DENSITY_DEFAULT.toFloat()
            Density(density = stableDensityValue, fontScale = textScale.factor)
        }
    CompositionLocalProvider(LocalDensity provides fixed, content = content)
}
