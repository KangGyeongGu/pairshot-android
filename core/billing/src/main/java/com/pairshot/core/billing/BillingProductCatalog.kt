package com.pairshot.core.billing

object BillingProductCatalog {
    const val PRO_SUBSCRIPTION = "pairshot_pro"

    const val BASE_PLAN_MONTHLY = "monthly"
    const val BASE_PLAN_YEARLY = "yearly"

    const val OFFER_TRIAL14 = "trial14"

    val subscriptionIds: List<String> = listOf(PRO_SUBSCRIPTION)
}
