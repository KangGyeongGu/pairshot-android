package com.pairshot.core.domain.pair

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetLatestBeforeThumbnailUseCase
@Inject
constructor(
    private val photoPairRepository: PhotoPairRepository,
) {
    operator fun invoke(): Flow<String?> =
        photoPairRepository
            .observeAll()
            .map { it.firstOrNull()?.beforePhotoUri }
}
