package com.pairshot.core.data.media

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import com.pairshot.core.domain.pair.GalleryImageMetadataReader
import com.pairshot.core.domain.pair.ImageDimensions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class GalleryImageMetadataReaderImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : GalleryImageMetadataReader {
    override suspend fun takenAtMs(uri: String): Long? =
        withContext(Dispatchers.IO) {
            runCatching {
                val projection =
                    arrayOf(
                        MediaStore.Images.Media.DATE_TAKEN,
                        MediaStore.Images.Media.DATE_ADDED,
                    )
                context.contentResolver
                    .query(Uri.parse(uri), projection, null, null, null)
                    ?.use { cursor ->
                        if (!cursor.moveToFirst()) return@use null
                        val takenIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                        val addedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                        val taken = if (takenIndex >= 0) cursor.getLong(takenIndex) else 0L
                        val added = if (addedIndex >= 0) cursor.getLong(addedIndex) else 0L
                        when {
                            taken > 0L -> taken
                            added > 0L -> added * MILLIS_PER_SECOND
                            else -> null
                        }
                    }
            }.onFailure {
                if (it is CancellationException) throw it
                Timber.w(it, "takenAtMs query failed: %s", uri)
            }
                .getOrNull()
        }

    override suspend fun dimensions(uri: String): ImageDimensions? =
        withContext(Dispatchers.IO) {
            runCatching {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(Uri.parse(uri))?.use { input ->
                    BitmapFactory.decodeStream(input, null, options)
                }
                if (options.outWidth > 0 && options.outHeight > 0) {
                    ImageDimensions(width = options.outWidth, height = options.outHeight)
                } else {
                    null
                }
            }.onFailure {
                if (it is CancellationException) throw it
                Timber.w(it, "dimensions read failed: %s", uri)
            }
                .getOrNull()
        }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
    }
}
