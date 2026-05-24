package com.pairshot.core.domain.album

import com.pairshot.core.domain.pair.PhotoPairRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DeleteAlbumUseCase
@Inject
constructor(
    private val albumRepository: AlbumRepository,
    private val photoPairRepository: PhotoPairRepository,
) {
    suspend operator fun invoke(albumId: Long) {
        val pairs = albumRepository.observePairs(albumId).first()
        pairs.forEach { pair ->
            runCatching { photoPairRepository.delete(pair) }
        }
        albumRepository.delete(albumId)
    }
}
