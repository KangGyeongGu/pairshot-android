package com.pairshot.core.ads.initializer

import android.content.Context
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdsInitializer
    @Inject
    constructor() {
        private val initialized = AtomicBoolean(false)
        private val ready = CompletableDeferred<Unit>()

        fun initialize(context: Context) {
            if (!initialized.compareAndSet(false, true)) return
            runCatching {
                MobileAds.initialize(context.applicationContext) { status ->
                    Timber.tag(TAG).d("MobileAds initialized: %s", status.adapterStatusMap)
                    ready.complete(Unit)
                }
            }.onFailure { error ->
                Timber.tag(TAG).e(error, "MobileAds initialize failed")
                ready.complete(Unit)
            }
        }

        suspend fun awaitReady() {
            withTimeoutOrNull(INIT_TIMEOUT_MS) { ready.await() }
        }

        private companion object {
            const val TAG = "AdsInitializer"
            const val INIT_TIMEOUT_MS = 5_000L
        }
    }
