package com.pairshot.core.data.export

import com.pairshot.core.data.device.DeviceCapability
import com.pairshot.core.data.device.MemoryGuard
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

private const val MEMORY_WAIT_DELAY_MS = 150L
private const val OOM_RETRY_DELAY_MS = 500L

@Singleton
class ExportPipeline
    @Inject
    constructor(
        capability: DeviceCapability,
        private val memoryGuard: MemoryGuard,
    ) {
        @OptIn(ExperimentalCoroutinesApi::class)
        private val cpuDispatcher: CoroutineDispatcher =
            Dispatchers.Default.limitedParallelism(capability.exportParallelism)

        private val memorySemaphore = Semaphore(capability.exportParallelism)

        suspend fun <T> processInParallel(
            total: Int,
            maxOutputPx: Int,
            onProgress: (current: Int, total: Int) -> Unit,
            block: suspend (index: Int) -> T?,
        ): List<T?> {
            val progress = AtomicInteger(0)
            return coroutineScope {
                (0 until total)
                    .map { index ->
                        async(cpuDispatcher) {
                            memorySemaphore.withPermit {
                                waitForMemoryAvailable(maxOutputPx)
                                try {
                                    runBlockWithOomRecovery(index, block)
                                } finally {
                                    onProgress(progress.incrementAndGet(), total)
                                }
                            }
                        }
                    }.awaitAll()
            }
        }

        private suspend fun waitForMemoryAvailable(maxOutputPx: Int) {
            while (!memoryGuard.canStartOneMore(maxOutputPx)) {
                delay(MEMORY_WAIT_DELAY_MS)
            }
        }

        private suspend fun <T> runBlockWithOomRecovery(
            index: Int,
            block: suspend (index: Int) -> T?,
        ): T? =
            try {
                block(index)
            } catch (_: OutOfMemoryError) {
                delay(OOM_RETRY_DELAY_MS)
                @Suppress("ExplicitGarbageCollectionCall")
                System.gc()
                runCatching { block(index) }.getOrNull()
            }
    }
