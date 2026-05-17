package com.pairshot.core.billing.di

import com.pairshot.core.billing.BillingRepository
import com.pairshot.core.billing.BuildConfig
import com.pairshot.core.billing.data.repository.BillingRepositoryImpl
import com.pairshot.core.billing.data.repository.FakeBillingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BillingModule {
    @Provides
    @Singleton
    fun provideBillingRepository(
        real: Provider<BillingRepositoryImpl>,
        fake: Provider<FakeBillingRepository>,
    ): BillingRepository = if (BuildConfig.DEBUG) fake.get() else real.get()
}
