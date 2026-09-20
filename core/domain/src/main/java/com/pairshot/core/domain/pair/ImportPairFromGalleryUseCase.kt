package com.pairshot.core.domain.pair

import com.pairshot.core.model.AspectRatio
import javax.inject.Inject

class ImportPairFromGalleryUseCase
@Inject
constructor(
    private val photoPairRepository: PhotoPairRepository,
    private val metadataReader: GalleryImageMetadataReader,
) {
    suspend operator fun invoke(
        beforeUri: String,
        afterUri: String? = null,
        albumId: Long? = null,
    ): Long {
        val beforeTakenAt = metadataReader.takenAtMs(beforeUri)
        val aspectRatio =
            metadataReader.dimensions(beforeUri)?.let { nearestAspectRatio(it.width, it.height) }
        val pairId =
            photoPairRepository.saveBeforePhoto(
                tempFileUri = beforeUri,
                zoomLevel = null,
                albumId = albumId,
                aspectRatio = aspectRatio,
                capturedAtMs = beforeTakenAt,
            )
        if (afterUri != null) {
            photoPairRepository.saveAfterPhoto(
                pairId = pairId,
                tempFileUri = afterUri,
                capturedAtMs = metadataReader.takenAtMs(afterUri),
            )
        }
        return pairId
    }

    companion object {
        private val CANDIDATES =
            listOf(
                AspectRatio.RATIO_4_3 to 4.0 / 3.0,
                AspectRatio.RATIO_16_9 to 16.0 / 9.0,
                AspectRatio.RATIO_1_1 to 1.0,
            )

        fun nearestAspectRatio(
            width: Int,
            height: Int,
        ): AspectRatio? {
            val short = minOf(width, height).toDouble()
            if (short <= 0.0) return null
            val ratio = maxOf(width, height) / short
            return CANDIDATES.minBy { (_, value) -> kotlin.math.abs(value - ratio) }.first
        }
    }
}
