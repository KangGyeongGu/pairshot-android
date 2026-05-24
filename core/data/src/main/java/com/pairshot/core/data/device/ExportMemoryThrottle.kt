package com.pairshot.core.data.device

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportMemoryThrottle
@Inject
constructor() : ComponentCallbacks2 {
    @Volatile
    private var trimLevel: Int = 0

    val currentTrimLevel: Int get() = trimLevel

    override fun onTrimMemory(level: Int) {
        trimLevel = level
    }

    override fun onConfigurationChanged(newConfig: Configuration) = Unit

    @Deprecated("Required override for legacy SDK", level = DeprecationLevel.WARNING)
    override fun onLowMemory() {
        trimLevel = ComponentCallbacks2.TRIM_MEMORY_COMPLETE
    }

    fun effectiveParallelism(staticN: Int): Int =
        when {
            trimLevel >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                1
            }

            trimLevel >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> {
                minOf(staticN, MODERATE_TRIM_MAX_PARALLELISM)
            }

            else -> {
                staticN
            }
        }

    companion object {
        private const val MODERATE_TRIM_MAX_PARALLELISM = 2
    }
}
