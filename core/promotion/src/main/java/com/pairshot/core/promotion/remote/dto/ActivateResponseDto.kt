package com.pairshot.core.promotion.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ActivateResponseDto(
    val promotion: MembershipPromotionDto,
    val membership: MembershipDto,
)
