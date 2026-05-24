package com.pairshot.core.rendering

import android.graphics.Bitmap
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.RenderProfile
import com.pairshot.core.model.WatermarkConfig
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CombinePreviewRenderer
@Inject
constructor(
    private val pairImageComposer: PairImageComposer,
    private val previewSampleProvider: PreviewSampleProvider,
) {
    suspend fun render(
        combineConfig: CombineConfig,
        watermarkConfig: WatermarkConfig,
    ): Bitmap? {
        val sample = previewSampleProvider.get()
        return runCatching {
            pairImageComposer.composeFromBitmaps(
                before = sample,
                after = sample,
                combineConfig = combineConfig,
                watermarkConfig = watermarkConfig,
                profile = RenderProfile.PREVIEW,
            )
        }.getOrNull()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CombinePreviewEntryPoint {
    fun combinePreviewRenderer(): CombinePreviewRenderer
}
