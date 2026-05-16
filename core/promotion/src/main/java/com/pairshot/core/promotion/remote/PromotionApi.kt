package com.pairshot.core.promotion.remote

import com.pairshot.core.promotion.remote.dto.ActivateRequestDto

interface PromotionApi {
    suspend fun fetchMembership(deviceHash: String): MembershipApiResult

    suspend fun activate(request: ActivateRequestDto): ActivationApiResult
}
