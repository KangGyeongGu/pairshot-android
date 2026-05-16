package com.pairshot.core.promotion.local

data class PendingPromotionActivation(
    val code: String,
    val sinceEpochMillis: Long,
)
