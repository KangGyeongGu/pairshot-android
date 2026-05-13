package com.pairshot.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pairshot.core.database.dao.AlbumDao
import com.pairshot.core.database.dao.ExportHistoryDao
import com.pairshot.core.database.dao.PairAlbumCrossRefDao
import com.pairshot.core.database.dao.PhotoPairDao
import com.pairshot.core.database.entity.AlbumEntity
import com.pairshot.core.database.entity.ExportHistoryEntity
import com.pairshot.core.database.entity.PairAlbumCrossRefEntity
import com.pairshot.core.database.entity.PhotoPairEntity

@Database(
    entities = [
        PhotoPairEntity::class,
        AlbumEntity::class,
        PairAlbumCrossRefEntity::class,
        ExportHistoryEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PairShotDatabase : RoomDatabase() {
    abstract fun photoPairDao(): PhotoPairDao

    abstract fun albumDao(): AlbumDao

    abstract fun pairAlbumCrossRefDao(): PairAlbumCrossRefDao

    abstract fun exportHistoryDao(): ExportHistoryDao
}
