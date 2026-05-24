package com.pairshot.core.data.repository

import com.pairshot.core.database.dao.AlbumDao
import com.pairshot.core.database.dao.PairAlbumCrossRefDao
import com.pairshot.core.database.entity.PairAlbumCrossRefEntity
import com.pairshot.core.database.entity.toDomain
import com.pairshot.core.database.entity.toEntity
import com.pairshot.core.domain.album.AlbumRepository
import com.pairshot.core.model.Album
import com.pairshot.core.model.PhotoPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AlbumRepositoryImpl
@Inject
constructor(
    private val albumDao: AlbumDao,
    private val pairAlbumCrossRefDao: PairAlbumCrossRefDao,
) : AlbumRepository {
    override fun getAll(): Flow<List<Album>> =
        albumDao.getAllByUpdated().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getById(id: Long): Album? =
        withContext(Dispatchers.IO) {
            albumDao.getById(id)?.toDomain()
        }

    override fun observePairs(albumId: Long): Flow<List<PhotoPair>> =
        pairAlbumCrossRefDao.getPairsByAlbum(albumId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun create(album: Album): Long =
        withContext(Dispatchers.IO) {
            albumDao.insert(album.toEntity())
        }

    override suspend fun update(album: Album) =
        withContext(Dispatchers.IO) {
            albumDao.update(album.toEntity())
        }

    override suspend fun delete(albumId: Long) {
        withContext(Dispatchers.IO) {
            albumDao.getById(albumId)?.let { albumDao.delete(it) }
        }
    }

    override suspend fun addPairs(
        albumId: Long,
        pairIds: List<Long>,
    ) = withContext(Dispatchers.IO) {
        val crossRefs =
            pairIds.map { pairId ->
                PairAlbumCrossRefEntity(pairId = pairId, albumId = albumId)
            }
        pairAlbumCrossRefDao.insertAll(crossRefs)
    }

    override suspend fun removePairs(
        albumId: Long,
        pairIds: List<Long>,
    ) = withContext(Dispatchers.IO) {
        pairAlbumCrossRefDao.deleteAll(albumId, pairIds)
    }

    override suspend fun getAlbumIdsForPair(pairId: Long): List<Long> =
        withContext(Dispatchers.IO) {
            pairAlbumCrossRefDao.getAlbumIdsForPair(pairId)
        }
}
