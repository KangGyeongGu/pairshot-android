package com.pairshot.core.promotion.domain

sealed interface ActivationResult {
    data class Success(
        val promotion: Promotion,
    ) : ActivationResult

    sealed interface Failure : ActivationResult {
        data object InvalidFormat : Failure

        data object InvalidSignature : Failure

        data object AlreadyUsedOnAnotherDevice : Failure

        data object Revoked : Failure

        data object NotFound : Failure

        data object NetworkError : Failure

        data object UnknownError : Failure
    }
}
