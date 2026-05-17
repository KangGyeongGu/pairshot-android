package com.pairshot.core.promotion.di

import com.pairshot.core.promotion.config.BuildConfigPromotionApiConfig
import com.pairshot.core.promotion.config.PromotionApiConfig
import com.pairshot.core.promotion.data.repository.PromotionRepositoryImpl
import com.pairshot.core.promotion.domain.PromotionRepository
import com.pairshot.core.promotion.remote.KtorPromotionApi
import com.pairshot.core.promotion.remote.PromotionApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PromotionModule {
    @Binds
    @Singleton
    abstract fun bindPromotionApiConfig(impl: BuildConfigPromotionApiConfig): PromotionApiConfig

    @Binds
    @Singleton
    abstract fun bindPromotionApi(impl: KtorPromotionApi): PromotionApi

    @Binds
    @Singleton
    abstract fun bindPromotionRepository(impl: PromotionRepositoryImpl): PromotionRepository
}
