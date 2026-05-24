package com.pairshot.core.data.repository

import android.net.Uri
import com.pairshot.core.database.dao.ExportHistoryDao
import com.pairshot.core.database.entity.toDomain
import com.pairshot.core.database.entity.toEntity
import com.pairshot.core.domain.combine.ExportHistoryRepository
import com.pairshot.core.model.ExportHistoryEntry
import com.pairshot.core.model.ExportHistoryKind
import com.pairshot.core.storage.MediaStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ExportHistoryRepositoryImpl
@Inject
constructor(
    private val exportHistoryDao: ExportHistoryDao,
    private val mediaStoreManager: MediaStoreManager,
) : ExportHistoryRepository {
    override suspend fun insert(entry: ExportHistoryEntry): Long =
        withContext(Dispatchers.IO) {
            val entity = entry.copy(createdAt = System.currentTimeMillis()).toEntity()
            exportHistoryDao.insert(entity)
        }

    override suspend fun findByPairIds(pairIds: List<Long>): List<ExportHistoryEntry> =
        withContext(Dispatchers.IO) {
            if (pairIds.isEmpty()) {
                emptyList()
            } else {
                exportHistoryDao.findByPairIds(pairIds).map { it.toDomain() }
            }
        }

    override suspend fun findByPairIdsAndKind(
        pairIds: List<Long>,
        kind: ExportHistoryKind,
    ): List<ExportHistoryEntry> =
        withContext(Dispatchers.IO) {
            if (pairIds.isEmpty()) {
                emptyList()
            } else {
                exportHistoryDao.findByPairIdsAndKind(pairIds, kind.name).map { it.toDomain() }
            }
        }

    override suspend fun deleteByPairIds(pairIds: List<Long>) {
        if (pairIds.isEmpty()) return
        withContext(Dispatchers.IO) {
            val rows = exportHistoryDao.findByPairIds(pairIds)
            rows.forEach { row ->
                runCatching { mediaStoreManager.deleteFromGallery(Uri.parse(row.mediaStoreUri)) }
            }
            exportHistoryDao.deleteByPairIds(pairIds)
        }
    }

    override suspend fun deleteByPairIdsAndKind(
        pairIds: List<Long>,
        kind: ExportHistoryKind,
    ) {
        if (pairIds.isEmpty()) return
        withContext(Dispatchers.IO) {
            val rows = exportHistoryDao.findByPairIdsAndKind(pairIds, kind.name)
            rows.forEach { row ->
                runCatching { mediaStoreManager.deleteFromGallery(Uri.parse(row.mediaStoreUri)) }
            }
            exportHistoryDao.deleteByPairIdsAndKind(pairIds, kind.name)
        }
    }
}
