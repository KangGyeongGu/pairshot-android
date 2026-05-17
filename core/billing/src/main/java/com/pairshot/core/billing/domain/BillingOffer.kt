package com.pairshot.core.billing.domain

data class BillingOffer(
    val productId: String,
    val basePlanId: String,
    val offerId: String?,
    val offerToken: String,
    val priceFormatted: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val billingPeriodIso: String,
    val trialDays: Int?,
)
