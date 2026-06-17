package com.pairshot.core.promotion.domain

import kotlinx.serialization.Serializable

@Serializable
data class Promotion(
    val id: String,
    val entitlement: PromotionEntitlement,
    val durationDays: Long?,
    val activatedAtEpochMillis: Long,
    val expiresAtEpochMillis: Long?,
    val status: PromotionStatus,
    val shortCode: String?,
    val batchLabel: String?,
)

@Serializable
enum class PromotionEntitlement { PRO }

@Serializable
enum class PromotionStatus { UNUSED, ACTIVATED, EXPIRED, REVOKED }
