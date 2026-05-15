package com.pairshot.core.domain.entitlement

data class ProEntitlement(
    val isActive: Boolean,
    val source: EntitlementSource,
    val expiresAtEpochMs: Long? = null,
)

enum class EntitlementSource { NONE, COUPON, SUBSCRIPTION }
