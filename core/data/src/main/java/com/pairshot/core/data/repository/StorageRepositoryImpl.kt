package com.pairshot.core.data.repository

import android.content.Context
import android.provider.MediaStore
import com.bumptech.glide.Glide
import com.pairshot.core.domain.settings.StorageRepository
import com.pairshot.core.model.StorageInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StorageRepositoryImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : StorageRepository {
    override suspend fun getStorageInfo(): StorageInfo =
        withContext(Dispatchers.IO) {
            val usedBytes = calculateMediaStoreUsage()
            val cacheBytes = calculateCacheSize()
            StorageInfo(usedBytes = usedBytes, cacheBytes = cacheBytes)
        }

    override suspend fun clearCache(): Long =
        withContext(Dispatchers.IO) {
            val before = calculateCacheSize()
            runCatching { Glide.get(context).clearDiskCache() }
            context.cacheDir.listFiles()?.forEach { file ->
                file.deleteRecursively()
            }
            val after = calculateCacheSize()
            before - after
        }

    private fun calculateMediaStoreUsage(): Long {
        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media.SIZE)
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("Pictures/PairShot/%")

        var totalSize = 0L
        resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            while (cursor.moveToNext()) {
                totalSize += cursor.getLong(sizeIndex)
            }
        }
        return totalSize
    }

    private fun calculateCacheSize(): Long =
        context.cacheDir
            .walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
}
