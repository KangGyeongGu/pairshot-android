package com.pairshot.core.domain.album

import javax.inject.Inject

class RemovePairsFromAlbumUseCase
@Inject
constructor(
    private val albumRepository: AlbumRepository,
) {
    suspend operator fun invoke(
        albumId: Long,
        pairIds: List<Long>,
    ) {
        if (pairIds.isEmpty()) return
        albumRepository.removePairs(albumId, pairIds)
    }
}
