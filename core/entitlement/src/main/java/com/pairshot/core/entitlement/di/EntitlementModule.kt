package com.pairshot.core.entitlement.di

import com.pairshot.core.domain.membership.MembershipProvider
import com.pairshot.core.entitlement.MembershipResolver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EntitlementModule {
    @Binds
    @Singleton
    abstract fun bindMembershipProvider(impl: MembershipResolver): MembershipProvider
}
