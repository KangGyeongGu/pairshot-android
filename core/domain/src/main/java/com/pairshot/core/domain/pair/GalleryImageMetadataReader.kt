package com.pairshot.core.domain.pair

interface GalleryImageMetadataReader {
    suspend fun takenAtMs(uri: String): Long?

    suspend fun dimensions(uri: String): ImageDimensions?
}

data class ImageDimensions(
    val width: Int,
    val height: Int,
)
