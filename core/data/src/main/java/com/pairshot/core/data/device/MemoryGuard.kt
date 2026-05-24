package com.pairshot.core.data.device

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryGuard
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    private val activityManager = context.getSystemService(ActivityManager::class.java)

    fun estimatePerPairPeakBytes(maxOutputPx: Int): Long {
        val px = if (maxOutputPx > 0) maxOutputPx else FULL_RESOLUTION_FALLBACK_PX
        return px.toLong() * px.toLong() * BYTES_PER_PIXEL * PEAK_MULTIPLIER
    }

    fun canStartOneMore(maxOutputPx: Int): Boolean {
        val memInfo =
            ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
        if (memInfo.lowMemory) return false
        val needed = estimatePerPairPeakBytes(maxOutputPx) * SAFETY_MARGIN_MULTIPLIER
        return memInfo.availMem > needed
    }

    companion object {
        private const val FULL_RESOLUTION_FALLBACK_PX = 4000
        private const val BYTES_PER_PIXEL = 4L
        private const val PEAK_MULTIPLIER = 4L
        private const val SAFETY_MARGIN_MULTIPLIER = 2L
    }
}
