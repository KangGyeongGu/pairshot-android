package com.pairshot.core.billing.domain

sealed interface PurchaseError {
    data object UserCanceled : PurchaseError

    data object AlreadyOwned : PurchaseError

    data object BillingUnavailable : PurchaseError

    data object ServiceDisconnected : PurchaseError

    data object ServiceUnavailable : PurchaseError

    data object ItemUnavailable : PurchaseError

    data object DeveloperError : PurchaseError

    data object NetworkError : PurchaseError

    data class Unknown(
        val responseCode: Int,
        val debugMessage: String,
    ) : PurchaseError
}
