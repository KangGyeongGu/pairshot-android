package com.pairshot.core.domain.entitlement

import kotlinx.coroutines.flow.Flow

interface ProEntitlementProvider {
    fun observe(): Flow<ProEntitlement>

    suspend fun current(): ProEntitlement
}
