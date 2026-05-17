package com.pairshot.core.data.di

import com.pairshot.core.data.repository.AlbumRepositoryImpl
import com.pairshot.core.data.repository.AppSettingsRepositoryImpl
import com.pairshot.core.data.repository.CombineSettingsRepositoryImpl
import com.pairshot.core.data.repository.ExportHistoryRepositoryImpl
import com.pairshot.core.data.repository.ExportRepositoryImpl
import com.pairshot.core.data.repository.OnboardingStateRepositoryImpl
import com.pairshot.core.data.repository.PhotoPairRepositoryImpl
import com.pairshot.core.data.repository.StorageRepositoryImpl
import com.pairshot.core.data.repository.WatermarkRepositoryImpl
import com.pairshot.core.domain.album.AlbumRepository
import com.pairshot.core.domain.combine.CombineSettingsRepository
import com.pairshot.core.domain.combine.ExportHistoryRepository
import com.pairshot.core.domain.export.ExportRepository
import com.pairshot.core.domain.pair.PhotoPairRepository
import com.pairshot.core.domain.settings.AppSettingsRepository
import com.pairshot.core.domain.settings.OnboardingStateRepository
import com.pairshot.core.domain.settings.StorageRepository
import com.pairshot.core.domain.settings.WatermarkRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAlbumRepository(impl: AlbumRepositoryImpl): AlbumRepository

    @Binds
    @Singleton
    abstract fun bindExportHistoryRepository(impl: ExportHistoryRepositoryImpl): ExportHistoryRepository

    @Binds
    @Singleton
    abstract fun bindExportRepository(impl: ExportRepositoryImpl): ExportRepository

    @Binds
    @Singleton
    abstract fun bindPhotoPairRepository(impl: PhotoPairRepositoryImpl): PhotoPairRepository

    @Binds
    @Singleton
    abstract fun bindAppSettingsRepository(impl: AppSettingsRepositoryImpl): AppSettingsRepository

    @Binds
    @Singleton
    abstract fun bindOnboardingStateRepository(impl: OnboardingStateRepositoryImpl): OnboardingStateRepository

    @Binds
    @Singleton
    abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository

    @Binds
    @Singleton
    abstract fun bindWatermarkRepository(impl: WatermarkRepositoryImpl): WatermarkRepository

    @Binds
    @Singleton
    abstract fun bindCombineSettingsRepository(impl: CombineSettingsRepositoryImpl): CombineSettingsRepository
}
