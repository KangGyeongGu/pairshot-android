package com.pairshot.core.data.export.internal

import com.pairshot.core.database.entity.PhotoPairEntity
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.ExportPreset
import com.pairshot.core.model.ImageQualityPreset
import com.pairshot.core.model.WatermarkConfig
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DecoratedPairMaterializer
@Inject
constructor(
    private val composer: CombinedImageComposer,
) {
    suspend fun materializePair(
        pair: PhotoPairEntity,
        seq: Int,
        preset: ExportPreset,
        combineConfig: CombineConfig,
        watermarkConfig: WatermarkConfig?,
        imageQuality: ImageQualityPreset,
        destDir: File,
        onFile: (destFile: File, subdir: String) -> Unit,
    ) {
        if (preset.includeBefore) {
            pair.validBeforeUriOrNull()?.let { uri ->
                val destFile = File(destDir, "BEFORE_%03d.jpg".format(seq))
                composer.materializeSingle(
                    uri,
                    destFile,
                    isBefore = true,
                    combineConfig,
                    watermarkConfig,
                    imageQuality
                )
                onFile(destFile, "before")
            }
        }
        if (preset.includeAfter) {
            pair.validAfterUriOrNull()?.let { uri ->
                val destFile = File(destDir, "AFTER_%03d.jpg".format(seq))
                composer.materializeSingle(
                    uri,
                    destFile,
                    isBefore = false,
                    combineConfig,
                    watermarkConfig,
                    imageQuality
                )
                onFile(destFile, "after")
            }
        }
        if (preset.includeCombined) {
            val before = pair.validBeforeUriOrNull()
            val after = pair.validAfterUriOrNull()
            if (before != null && after != null) {
                val destFile = File(destDir, "PAIR_%03d.jpg".format(seq))
                composer.composeCombinedFile(before, after, destFile, combineConfig, watermarkConfig, imageQuality)
                onFile(destFile, "combined")
            }
        }
    }
}
