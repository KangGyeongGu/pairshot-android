package com.pairshot.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pairshot.core.database.entity.PhotoPairEntity
import com.pairshot.core.database.entity.PhotoPairWithCountsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoPairDao {
    @Query(
        """
        SELECT
            pp.*,
            EXISTS(SELECT 1 FROM export_history eh WHERE eh.pairId = pp.id AND eh.kind = 'COMBINED') AS hasCombined
        FROM photo_pairs pp
        ORDER BY pp.beforeTimestamp ASC
        """,
    )
    fun observeAllWithCounts(): Flow<List<PhotoPairWithCountsEntity>>

    @Query("SELECT * FROM photo_pairs WHERE status = 'BEFORE_ONLY' ORDER BY beforeTimestamp DESC")
    fun observeUnpaired(): Flow<List<PhotoPairEntity>>

    @Query("SELECT * FROM photo_pairs WHERE id = :id")
    suspend fun getById(id: Long): PhotoPairEntity?

    @Query("SELECT * FROM photo_pairs WHERE id = :id")
    fun observeById(id: Long): Flow<PhotoPairEntity?>

    @Query("SELECT * FROM photo_pairs WHERE id IN (:ids) ORDER BY beforeTimestamp ASC")
    suspend fun getByIds(ids: List<Long>): List<PhotoPairEntity>

    @Insert
    suspend fun insert(pair: PhotoPairEntity): Long

    @Update
    suspend fun update(pair: PhotoPairEntity)

    @Delete
    suspend fun delete(pair: PhotoPairEntity)

    @Query("SELECT COUNT(*) FROM photo_pairs")
    fun countAll(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photo_pairs WHERE beforeTimestamp >= :sinceEpochMs")
    fun countCreatedSince(sinceEpochMs: Long): Flow<Int>

    @Query("SELECT MAX(id) FROM photo_pairs")
    suspend fun getMaxId(): Long?
}
