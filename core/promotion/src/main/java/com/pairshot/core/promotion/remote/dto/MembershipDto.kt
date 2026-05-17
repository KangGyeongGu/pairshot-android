package com.pairshot.core.promotion.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MembershipDto(
    val pro: EntitlementInfoDto,
    val adFree: EntitlementInfoDto,
    val promotions: List<MembershipPromotionDto>,
)

@Serializable
data class EntitlementInfoDto(
    val active: Boolean,
    val expiresAt: String? = null,
)

@Serializable
data class MembershipPromotionDto(
    val id: String,
    val entitlement: String,
    val durationDays: Long? = null,
    val activatedAt: String,
    val expiresAt: String? = null,
    val status: String,
    val shortCode: String? = null,
    val batchLabel: String? = null,
)
