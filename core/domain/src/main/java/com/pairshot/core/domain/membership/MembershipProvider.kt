package com.pairshot.core.domain.membership

import kotlinx.coroutines.flow.Flow

interface MembershipProvider {
    fun observe(): Flow<Membership>

    suspend fun current(): Membership
}
