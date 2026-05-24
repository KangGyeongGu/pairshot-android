package com.pairshot.core.domain.settings

import javax.inject.Inject

class ClearCacheUseCase
@Inject
constructor(
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(): Long = storageRepository.clearCache()
}
