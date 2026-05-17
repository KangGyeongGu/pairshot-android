package com.pairshot.core.domain.pair

import com.pairshot.core.model.AspectRatio
import com.pairshot.core.model.PhotoPair
import kotlinx.coroutines.flow.Flow

interface PhotoPairRepository {
    fun observeAll(): Flow<List<PhotoPair>>

    fun observeUnpaired(): Flow<List<PhotoPair>>

    fun observeUnpairedByAlbum(albumId: Long): Flow<List<PhotoPair>>

    suspend fun getById(id: Long): PhotoPair?

    fun observeById(id: Long): Flow<PhotoPair?>

    suspend fun getByIds(ids: List<Long>): List<PhotoPair>

    suspend fun delete(pair: PhotoPair)

    fun countAll(): Flow<Int>

    fun countCreatedSince(sinceEpochMs: Long): Flow<Int>

    suspend fun saveBeforePhoto(
        tempFileUri: String,
        zoomLevel: Float?,
        albumId: Long? = null,
        aspectRatio: AspectRatio? = null,
    ): Long

    suspend fun saveAfterPhoto(
        pairId: Long,
        tempFileUri: String,
    )

    suspend fun replaceBeforePhoto(
        pairId: Long,
        tempFileUri: String,
    )

    suspend fun pruneMissingSources(pairId: Long): PrunePairResult

    suspend fun pruneAllMissingSources(): List<PrunePairResult>
}
