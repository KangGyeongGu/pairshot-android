package com.pairshot.feature.tutorial.internal

import com.pairshot.core.datastore.AppPreferences
import com.pairshot.core.domain.pair.PhotoPairRepository
import com.pairshot.core.domain.tutorial.TutorialPairTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class TutorialSandbox
    @Inject
    constructor(
        private val photoPairRepositoryProvider: Provider<PhotoPairRepository>,
        private val appPreferences: AppPreferences,
    ) : TutorialPairTracker {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val mutex = Mutex()
        override val trackedPairIds: StateFlow<Set<Long>> =
            appPreferences.tutorialSandboxPairIds
                .stateIn(scope, SharingStarted.Eagerly, emptySet())

        override suspend fun registerPair(pairId: Long) {
            mutex.withLock {
                val current = appPreferences.tutorialSandboxPairIds.first()
                appPreferences.setTutorialSandboxPairIds(current + pairId)
            }
            Timber.tag(TAG).d("registerPair: %d", pairId)
        }

        override suspend fun registerTempFile(path: String) {
            mutex.withLock {
                val current = appPreferences.tutorialSandboxTempFiles.first()
                appPreferences.setTutorialSandboxTempFiles(current + path)
            }
        }

        suspend fun cleanup() {
            val pairsToDelete: Set<Long>
            val filesToDelete: Set<String>
            mutex.withLock {
                pairsToDelete = appPreferences.tutorialSandboxPairIds.first()
                filesToDelete = appPreferences.tutorialSandboxTempFiles.first()
            }
            Timber.tag(TAG).d(
                "cleanup: deleting %d pair(s), %d temp file(s)",
                pairsToDelete.size,
                filesToDelete.size,
            )
            withContext(Dispatchers.IO) {
                val repo = photoPairRepositoryProvider.get()
                pairsToDelete.forEach { id ->
                    runCatching {
                        val pair = repo.getById(id)
                        if (pair == null) {
                            Timber.tag(TAG).w("sandbox pair not found in DB: %d", id)
                        } else {
                            repo.delete(pair)
                        }
                    }.onFailure { Timber.tag(TAG).w(it, "sandbox pair delete failed: %d", id) }
                }
                filesToDelete.forEach { path ->
                    runCatching {
                        val file = File(path)
                        if (file.exists()) file.delete()
                    }.onFailure { Timber.tag(TAG).w(it, "sandbox temp delete failed: %s", path) }
                }
            }
            mutex.withLock {
                appPreferences.setTutorialSandboxPairIds(emptySet())
                appPreferences.setTutorialSandboxTempFiles(emptySet())
            }
        }

        companion object {
            private const val TAG = "Tutorial"
        }
    }
