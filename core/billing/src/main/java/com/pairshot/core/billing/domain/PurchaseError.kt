package com.pairshot.core.billing.domain

sealed interface PurchaseError {
    data object UserCanceled : PurchaseError

    data object Disconnected : PurchaseError

    data object NotReady : PurchaseError

    data object ItemUnavailable : PurchaseError

    data object NetworkError : PurchaseError

    data class Unknown(
        val responseCode: Int,
        val debugMessage: String,
    ) : PurchaseError
}
