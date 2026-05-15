package com.pairshot.core.data.repository

import android.database.sqlite.SQLiteException
import android.net.Uri
import com.pairshot.core.database.dao.PairAlbumCrossRefDao
import com.pairshot.core.database.dao.PhotoPairDao
import com.pairshot.core.database.entity.PairAlbumCrossRefEntity
import com.pairshot.core.database.entity.PhotoPairEntity
import com.pairshot.core.database.entity.toDomain
import com.pairshot.core.database.entity.toEntity
import com.pairshot.core.domain.pair.PhotoPairRepository
import com.pairshot.core.domain.pair.PrunePairResult
import com.pairshot.core.domain.settings.AppSettingsRepository
import com.pairshot.core.model.AspectRatio
import com.pairshot.core.model.PairStatus
import com.pairshot.core.model.PhotoPair
import com.pairshot.core.rendering.FileNameGenerator
import com.pairshot.core.storage.DeleteResult
import com.pairshot.core.storage.MediaSourceVerifier
import com.pairshot.core.storage.MediaStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class PhotoPairRepositoryImpl
    @Inject
    constructor(
        private val photoPairDao: PhotoPairDao,
        private val pairAlbumCrossRefDao: PairAlbumCrossRefDao,
        private val mediaStoreManager: MediaStoreManager,
        private val mediaSourceVerifier: MediaSourceVerifier,
        private val fileNameGenerator: FileNameGenerator,
        private val appSettingsRepository: AppSettingsRepository,
    ) : PhotoPairRepository {
        override fun observeAll(): Flow<List<PhotoPair>> =
            photoPairDao.observeAllWithCounts().map { entities ->
                entities.map { it.toDomain() }
            }

        override fun observeUnpaired(): Flow<List<PhotoPair>> =
            photoPairDao.observeUnpaired().map { entities ->
                entities.map { it.toDomain() }
            }

        override fun observeUnpairedByAlbum(albumId: Long): Flow<List<PhotoPair>> =
            pairAlbumCrossRefDao.getUnpairedByAlbum(albumId).map { entities ->
                entities.map { it.toDomain() }
            }

        override suspend fun getById(id: Long): PhotoPair? =
            withContext(Dispatchers.IO) {
                photoPairDao.getById(id)?.toDomain()
            }

        override fun observeById(id: Long): Flow<PhotoPair?> = photoPairDao.observeById(id).map { entity -> entity?.toDomain() }

        override suspend fun getByIds(ids: List<Long>): List<PhotoPair> =
            withContext(Dispatchers.IO) {
                if (ids.isEmpty()) emptyList() else photoPairDao.getByIds(ids).map { it.toDomain() }
            }

        override suspend fun pruneMissingSources(pairId: Long): PrunePairResult =
            withContext(Dispatchers.IO) {
                val entity = photoPairDao.getById(pairId) ?: return@withContext PrunePairResult.NotFound
                val beforeAlive = mediaSourceVerifier.exists(entity.beforePhotoUri)
                val afterAlive = entity.afterPhotoUri?.let { mediaSourceVerifier.exists(it) } == true
                applyPrune(entity, beforeAlive, afterAlive)
            }

        override suspend fun pruneAllMissingSources(): List<PrunePairResult> =
            withContext(Dispatchers.IO) {
                photoPairDao.observeAllWithCounts().first().map { withCounts ->
                    val entity = withCounts.pair
                    val beforeAlive = mediaSourceVerifier.exists(entity.beforePhotoUri)
                    val afterAlive = entity.afterPhotoUri?.let { mediaSourceVerifier.exists(it) } == true
                    applyPrune(entity, beforeAlive, afterAlive)
                }
            }

        private suspend fun applyPrune(
            entity: PhotoPairEntity,
            beforeAlive: Boolean,
            afterAlive: Boolean,
        ): PrunePairResult {
            val hadBefore = !entity.beforePhotoUri.isNullOrBlank()
            val hadAfter = !entity.afterPhotoUri.isNullOrBlank()
            val beforeMissing = hadBefore && !beforeAlive
            val afterMissing = hadAfter && !afterAlive
            if (!beforeMissing && !afterMissing) return PrunePairResult.Healthy

            val beforeRemains = hadBefore && !beforeMissing
            val afterRemains = hadAfter && !afterMissing
            if (!beforeRemains && !afterRemains) {
                photoPairDao.delete(entity)
                return PrunePairResult.DeletedEntirely
            }

            photoPairDao.update(buildPrunedEntity(entity, beforeRemains, afterRemains))
            return resolvePruneResult(beforeMissing, afterMissing)
        }

        private fun buildPrunedEntity(
            entity: PhotoPairEntity,
            beforeRemains: Boolean,
            afterRemains: Boolean,
        ): PhotoPairEntity =
            entity.copy(
                beforePhotoUri = if (beforeRemains) entity.beforePhotoUri else null,
                afterPhotoUri = if (afterRemains) entity.afterPhotoUri else null,
                afterTimestamp = if (afterRemains) entity.afterTimestamp else null,
                status = resolveStatus(beforeRemains, afterRemains).name,
            )

        private fun resolveStatus(
            beforeRemains: Boolean,
            afterRemains: Boolean,
        ): PairStatus =
            when {
                beforeRemains && afterRemains -> PairStatus.PAIRED
                beforeRemains -> PairStatus.BEFORE_ONLY
                else -> PairStatus.AFTER_ONLY
            }

        private fun resolvePruneResult(
            beforeMissing: Boolean,
            afterMissing: Boolean,
        ): PrunePairResult =
            when {
                beforeMissing && afterMissing -> PrunePairResult.DeletedEntirely
                beforeMissing -> PrunePairResult.BeforeDropped
                else -> PrunePairResult.AfterDropped
            }

        override suspend fun delete(pair: PhotoPair) {
            withContext(Dispatchers.IO) {
                pair.beforePhotoUri?.let {
                    deleteGalleryUriLogOnFailure(it, "before delete failed during pair removal")
                }
                pair.afterPhotoUri?.let {
                    deleteGalleryUriLogOnFailure(it, "after delete failed during pair removal")
                }
                photoPairDao.delete(pair.toEntity())
            }
        }

        override fun countAll(): Flow<Int> = photoPairDao.countAll()

        override fun countCreatedSince(sinceEpochMs: Long): Flow<Int> = photoPairDao.countCreatedSince(sinceEpochMs)

        override suspend fun saveBeforePhoto(
            tempFileUri: String,
            zoomLevel: Float?,
            albumId: Long?,
            aspectRatio: AspectRatio?,
        ): Long =
            withContext(Dispatchers.IO) {
                try {
                    val sequenceNumber = (photoPairDao.getMaxId() ?: 0L).plus(1L).toInt()

                    val prefix = appSettingsRepository.getCurrent().fileNamePrefix
                    val fileName = fileNameGenerator.generateBeforeFileName(sequenceNumber, prefix)

                    val savedUri =
                        mediaStoreManager.saveToGallery(
                            tempFileUri = Uri.parse(tempFileUri),
                            subfolder = "",
                            displayName = fileName,
                        )

                    val entity =
                        PhotoPairEntity(
                            beforePhotoUri = savedUri.toString(),
                            beforeTimestamp = System.currentTimeMillis(),
                            status = PairStatus.BEFORE_ONLY.name,
                            zoomLevel = zoomLevel,
                            aspectRatio = aspectRatio?.name,
                        )
                    try {
                        val pairId = photoPairDao.insert(entity)
                        if (albumId != null) {
                            pairAlbumCrossRefDao.insert(
                                PairAlbumCrossRefEntity(
                                    pairId = pairId,
                                    albumId = albumId,
                                ),
                            )
                        }
                        pairId
                    } catch (e: SQLiteException) {
                        deleteGalleryUriLogOnFailure(savedUri.toString(), "BEFORE rollback failed")
                        throw e
                    } catch (e: IllegalStateException) {
                        deleteGalleryUriLogOnFailure(savedUri.toString(), "BEFORE rollback failed")
                        throw e
                    }
                } finally {
                    deleteTempFile(tempFileUri)
                }
            }

        override suspend fun saveAfterPhoto(
            pairId: Long,
            tempFileUri: String,
        ) = withContext(Dispatchers.IO) {
            try {
                val entity =
                    photoPairDao.getById(pairId)
                        ?: throw IllegalArgumentException("PhotoPair not found: $pairId")

                entity.afterPhotoUri?.let { deleteGalleryUriLogOnFailure(it, "previous After photo delete failed") }

                val sequenceNumber = extractSequenceNumber(entity.beforePhotoUri.orEmpty())
                val prefix = appSettingsRepository.getCurrent().fileNamePrefix
                val fileName = fileNameGenerator.generateAfterFileName(sequenceNumber, prefix)

                val savedUri =
                    mediaStoreManager.saveToGallery(
                        tempFileUri = Uri.parse(tempFileUri),
                        subfolder = "",
                        displayName = fileName,
                    )

                try {
                    val now = System.currentTimeMillis()
                    photoPairDao.update(
                        entity.copy(
                            afterPhotoUri = savedUri.toString(),
                            afterTimestamp = now,
                            status = PairStatus.PAIRED.name,
                        ),
                    )
                } catch (e: SQLiteException) {
                    deleteGalleryUriLogOnFailure(savedUri.toString(), "AFTER rollback failed")
                    throw e
                } catch (e: IllegalStateException) {
                    deleteGalleryUriLogOnFailure(savedUri.toString(), "AFTER rollback failed")
                    throw e
                }
            } finally {
                deleteTempFile(tempFileUri)
            }
        }

        override suspend fun replaceBeforePhoto(
            pairId: Long,
            tempFileUri: String,
        ) = withContext(Dispatchers.IO) {
            try {
                val entity =
                    photoPairDao.getById(pairId)
                        ?: throw IllegalArgumentException("PhotoPair not found: $pairId")

                entity.beforePhotoUri?.let {
                    deleteGalleryUriLogOnFailure(it, "previous Before photo delete failed")
                }

                val sequenceNumber = extractSequenceNumber(entity.beforePhotoUri.orEmpty())
                val prefix = appSettingsRepository.getCurrent().fileNamePrefix
                val fileName = fileNameGenerator.generateBeforeFileName(sequenceNumber, prefix)

                val savedUri =
                    mediaStoreManager.saveToGallery(
                        tempFileUri = Uri.parse(tempFileUri),
                        subfolder = "",
                        displayName = fileName,
                    )

                try {
                    val now = System.currentTimeMillis()
                    val newStatus =
                        if (entity.afterPhotoUri.isNullOrBlank()) {
                            PairStatus.BEFORE_ONLY
                        } else {
                            PairStatus.PAIRED
                        }
                    photoPairDao.update(
                        entity.copy(
                            beforePhotoUri = savedUri.toString(),
                            beforeTimestamp = now,
                            status = newStatus.name,
                        ),
                    )
                } catch (e: SQLiteException) {
                    deleteGalleryUriLogOnFailure(savedUri.toString(), "BEFORE replace rollback failed")
                    throw e
                } catch (e: IllegalStateException) {
                    deleteGalleryUriLogOnFailure(savedUri.toString(), "BEFORE replace rollback failed")
                    throw e
                }
            } finally {
                deleteTempFile(tempFileUri)
            }
        }

        private fun deleteGalleryUriLogOnFailure(
            uriString: String,
            failureLogMessage: String,
        ) {
            val result = mediaStoreManager.deleteFromGallery(Uri.parse(uriString))
            when (result) {
                is DeleteResult.Success, DeleteResult.NotFound -> {
                    Unit
                }

                is DeleteResult.Failed -> {
                    Timber.w(result.exception, "$failureLogMessage: $uriString")
                }

                is DeleteResult.RecoverablePermission -> {
                    Timber.w(result.exception, "$failureLogMessage (permission required): $uriString")
                }
            }
        }

        private fun extractSequenceNumber(beforePhotoUri: String): Int {
            val match = Regex("BEFORE_(\\d+)_").find(beforePhotoUri)
            return match?.groupValues?.get(1)?.toIntOrNull() ?: 1
        }

        private fun deleteTempFile(tempFileUri: String) {
            try {
                val uri = Uri.parse(tempFileUri)
                val path =
                    when (uri.scheme) {
                        "file" -> uri.path
                        null -> tempFileUri
                        else -> null
                    }
                if (path != null) java.io.File(path).delete()
            } catch (e: SecurityException) {
                Timber.d(e, "temp file delete failed: $tempFileUri")
            } catch (e: IOException) {
                Timber.d(e, "temp file delete failed: $tempFileUri")
            }
        }
    }
