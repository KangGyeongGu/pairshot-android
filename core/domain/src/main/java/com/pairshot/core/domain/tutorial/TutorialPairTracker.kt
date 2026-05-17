package com.pairshot.core.domain.tutorial

import kotlinx.coroutines.flow.StateFlow

interface TutorialPairTracker {
    val trackedPairIds: StateFlow<Set<Long>>

    suspend fun registerPair(pairId: Long)

    suspend fun registerTempFile(path: String)
}
