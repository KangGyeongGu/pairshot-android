package com.pairshot.core.entitlement.di

import com.pairshot.core.domain.entitlement.ProEntitlementProvider
import com.pairshot.core.entitlement.CompositeProEntitlementProvider
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
    abstract fun bindProEntitlementProvider(impl: CompositeProEntitlementProvider): ProEntitlementProvider
}
