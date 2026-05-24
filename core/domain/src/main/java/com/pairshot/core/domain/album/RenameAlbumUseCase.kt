package com.pairshot.core.domain.album

import javax.inject.Inject

class RenameAlbumUseCase
@Inject
constructor(
    private val albumRepository: AlbumRepository,
) {
    suspend operator fun invoke(
        albumId: Long,
        newName: String,
    ) {
        require(newName.isNotBlank())
        val album = albumRepository.getById(albumId) ?: return
        albumRepository.update(
            album.copy(
                name = newName.trim(),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }
}
