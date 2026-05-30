package com.pairshot.core.data.export.internal

import com.pairshot.core.domain.export.ExportItem
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.ImageQualityPreset
import com.pairshot.core.model.PhotoPair
import com.pairshot.core.model.WatermarkConfig
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class RenderedItem(
    val item: ExportItem,
    val file: File,
    val subdir: String,
)

@Singleton
class ExportItemRenderer
@Inject
constructor(
    private val composer: CombinedImageComposer,
) {
    suspend fun render(
        item: ExportItem,
        pair: PhotoPair,
        seq: Int,
        destDir: File,
        combineConfig: CombineConfig,
        watermarkConfig: WatermarkConfig?,
        imageQuality: ImageQualityPreset,
    ): RenderedItem? =
        when (item) {
            is ExportItem.CombinedImage -> {
                val before = pair.beforePhotoUri
                val after = pair.afterPhotoUri
                if (before.isNullOrBlank() || after.isNullOrBlank()) {
                    null
                } else {
                    val destFile = File(destDir, "PAIR_%03d.jpg".format(seq))
                    composer.composeCombinedFile(
                        beforeUri = before,
                        afterUri = after,
                        destFile = destFile,
                        combineConfig = combineConfig,
                        watermarkConfig = watermarkConfig,
                        imageQuality = imageQuality,
                    )
                    RenderedItem(item = item, file = destFile, subdir = "combined")
                }
            }

            is ExportItem.BeforeImage -> {
                val destFile = File(destDir, "BEFORE_%03d.jpg".format(seq))
                composer.materializeSingle(
                    sourceUri = item.sourceUri,
                    destFile = destFile,
                    isBefore = true,
                    combineConfig = combineConfig,
                    watermarkConfig = watermarkConfig,
                    imageQuality = imageQuality,
                )
                RenderedItem(item = item, file = destFile, subdir = "before")
            }

            is ExportItem.AfterImage -> {
                val destFile = File(destDir, "AFTER_%03d.jpg".format(seq))
                composer.materializeSingle(
                    sourceUri = item.sourceUri,
                    destFile = destFile,
                    isBefore = false,
                    combineConfig = combineConfig,
                    watermarkConfig = watermarkConfig,
                    imageQuality = imageQuality,
                )
                RenderedItem(item = item, file = destFile, subdir = "after")
            }
        }
}
