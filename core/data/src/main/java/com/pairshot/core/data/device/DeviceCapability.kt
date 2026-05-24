package com.pairshot.core.data.device

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class DeviceTier { SAFE, BALANCED, PERFORMANCE }

@Singleton
class DeviceCapability
@Inject
constructor(
    @ApplicationContext context: Context,
) {
    private val activityManager = context.getSystemService(ActivityManager::class.java)
    private val memInfo =
        ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }

    val totalRamMb: Long = memInfo.totalMem / BYTES_PER_MB
    val isOsLowRam: Boolean = activityManager.isLowRamDevice
    val memoryClassMb: Int = activityManager.memoryClass
    val cores: Int = Runtime.getRuntime().availableProcessors()

    val tier: DeviceTier =
        when {
            isOsLowRam || totalRamMb < SAFE_TIER_MAX_RAM_MB -> {
                DeviceTier.SAFE
            }

            totalRamMb >= PERFORMANCE_TIER_MIN_RAM_MB && cores >= PERFORMANCE_TIER_MIN_CORES -> {
                DeviceTier.PERFORMANCE
            }

            else -> {
                DeviceTier.BALANCED
            }
        }

    val exportParallelism: Int =
        when (tier) {
            DeviceTier.SAFE -> SAFE_PARALLELISM
            DeviceTier.BALANCED -> BALANCED_PARALLELISM
            DeviceTier.PERFORMANCE -> PERFORMANCE_PARALLELISM
        }

    companion object {
        private const val BYTES_PER_MB = 1024L * 1024L
        private const val SAFE_TIER_MAX_RAM_MB = 3_072L
        private const val PERFORMANCE_TIER_MIN_RAM_MB = 8_192L
        private const val PERFORMANCE_TIER_MIN_CORES = 8
        private const val SAFE_PARALLELISM = 1
        private const val BALANCED_PARALLELISM = 2
        private const val PERFORMANCE_PARALLELISM = 3
    }
}
