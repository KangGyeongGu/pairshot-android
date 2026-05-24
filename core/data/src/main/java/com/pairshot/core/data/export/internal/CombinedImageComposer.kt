package com.pairshot.core.data.export.internal

import android.net.Uri
import com.pairshot.core.data.export.ShareImagePreparer
import com.pairshot.core.data.export.WatermarkedBitmapWriter
import com.pairshot.core.domain.export.needsIndividualDecoration
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.ImageQualityPreset
import com.pairshot.core.model.RenderProfile
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.rendering.PairImageComposer
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CombinedImageComposer
@Inject
constructor(
    private val pairImageComposer: PairImageComposer,
    private val watermarkedBitmapWriter: WatermarkedBitmapWriter,
    private val shareImagePreparer: ShareImagePreparer,
) {
    suspend fun composeCombinedFile(
        beforeUri: String,
        afterUri: String,
        destFile: File,
        combineConfig: CombineConfig,
        watermarkConfig: WatermarkConfig?,
        imageQuality: ImageQualityPreset,
    ) {
        val profile = RenderProfile.forExport(imageQuality)
        if (watermarkConfig != null) {
            watermarkedBitmapWriter.combineWithWatermark(
                beforeUri = beforeUri,
                afterUri = afterUri,
                destFile = destFile,
                config = watermarkConfig,
                jpegQuality = imageQuality.jpegQuality,
                combineConfig = combineConfig,
                profile = profile,
            )
        } else {
            pairImageComposer.composeToFile(
                beforeUri = Uri.parse(beforeUri),
                afterUri = Uri.parse(afterUri),
                destFile = destFile,
                combineConfig = combineConfig,
                watermarkConfig = WatermarkConfig(),
                jpegQuality = imageQuality.jpegQuality,
                profile = profile,
            )
        }
    }

    suspend fun materializeSingle(
        sourceUri: String,
        destFile: File,
        isBefore: Boolean,
        combineConfig: CombineConfig,
        watermarkConfig: WatermarkConfig?,
        imageQuality: ImageQualityPreset,
    ) {
        if (needsIndividualDecoration(combineConfig, watermarkConfig)) {
            pairImageComposer.composeSingleToFile(
                sourceUri = Uri.parse(sourceUri),
                destFile = destFile,
                isBefore = isBefore,
                combineConfig = combineConfig,
                watermarkConfig = watermarkConfig ?: WatermarkConfig(),
                jpegQuality = imageQuality.jpegQuality,
                profile = RenderProfile.forExport(imageQuality),
            )
        } else {
            shareImagePreparer.copyFromContentUri(sourceUri, destFile)
        }
    }
}
