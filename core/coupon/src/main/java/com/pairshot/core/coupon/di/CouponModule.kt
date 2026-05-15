package com.pairshot.core.coupon.di

import com.pairshot.core.coupon.config.BuildConfigCouponApiConfig
import com.pairshot.core.coupon.config.CouponApiConfig
import com.pairshot.core.coupon.data.repository.CouponRepositoryImpl
import com.pairshot.core.coupon.domain.CouponRepository
import com.pairshot.core.coupon.remote.CouponActivationApi
import com.pairshot.core.coupon.remote.KtorCouponActivationApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CouponModule {
    @Binds
    @Singleton
    abstract fun bindCouponApiConfig(impl: BuildConfigCouponApiConfig): CouponApiConfig

    @Binds
    @Singleton
    abstract fun bindCouponActivationApi(impl: KtorCouponActivationApi): CouponActivationApi

    @Binds
    @Singleton
    abstract fun bindCouponRepository(impl: CouponRepositoryImpl): CouponRepository
}
