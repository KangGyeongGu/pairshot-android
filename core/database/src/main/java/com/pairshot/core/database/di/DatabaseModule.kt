package com.pairshot.core.database.di

import android.content.Context
import androidx.room.Room
import com.pairshot.core.database.PairShotDatabase
import com.pairshot.core.database.dao.AlbumDao
import com.pairshot.core.database.dao.ExportHistoryDao
import com.pairshot.core.database.dao.PairAlbumCrossRefDao
import com.pairshot.core.database.dao.PhotoPairDao
import com.pairshot.core.database.migration.MIGRATION_1_2
import com.pairshot.core.database.migration.MIGRATION_2_3
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): PairShotDatabase =
        Room
            .databaseBuilder(context, PairShotDatabase::class.java, "pairshot.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    @Singleton
    fun providePhotoPairDao(db: PairShotDatabase): PhotoPairDao = db.photoPairDao()

    @Provides
    @Singleton
    fun provideAlbumDao(db: PairShotDatabase): AlbumDao = db.albumDao()

    @Provides
    @Singleton
    fun providePairAlbumCrossRefDao(db: PairShotDatabase): PairAlbumCrossRefDao = db.pairAlbumCrossRefDao()

    @Provides
    @Singleton
    fun provideExportHistoryDao(db: PairShotDatabase): ExportHistoryDao = db.exportHistoryDao()
}
