package com.pairshot.core.promotion.remote

import com.pairshot.core.promotion.remote.dto.ActivateResponseDto
import com.pairshot.core.promotion.remote.dto.MembershipDto

sealed interface MembershipApiResult {
    data class Success(val membership: MembershipDto) : MembershipApiResult

    data object NetworkError : MembershipApiResult

    data object ServerError : MembershipApiResult
}

sealed interface ActivationApiResult {
    data class Success(val response: ActivateResponseDto) : ActivationApiResult

    data object InvalidCodeFormat : ActivationApiResult

    data object InvalidSignature : ActivationApiResult

    data object NotFound : ActivationApiResult

    data object Revoked : ActivationApiResult

    data object AlreadyUsedOnAnotherDevice : ActivationApiResult

    data object NetworkError : ActivationApiResult

    data object ServerError : ActivationApiResult
}
