package com.pairshot.core.domain.album

import com.pairshot.core.model.Album
import javax.inject.Inject

class CreateAlbumUseCase
@Inject
constructor(
    private val albumRepository: AlbumRepository,
) {
    suspend operator fun invoke(
        name: String,
        address: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
    ): Long {
        val now = System.currentTimeMillis()
        val album =
            Album(
                name = name,
                address = address,
                latitude = latitude,
                longitude = longitude,
                createdAt = now,
                updatedAt = now,
            )
        return albumRepository.create(album)
    }
}
