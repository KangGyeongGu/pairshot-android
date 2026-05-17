package com.pairshot.core.storage

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed class DeleteResult {
    data object Success : DeleteResult()

    data object NotFound : DeleteResult()

    data class RecoverablePermission(
        val exception: RecoverableSecurityException,
    ) : DeleteResult()

    data class Failed(
        val exception: Exception,
    ) : DeleteResult()
}

@Singleton
class MediaStoreManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun saveToGallery(
            tempFileUri: Uri,
            subfolder: String,
            displayName: String,
        ): Uri {
            val resolver = context.contentResolver

            val imageCollection =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

            val imageDetails =
                ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val relativePath =
                            if (subfolder.isBlank()) "Pictures/PairShot/" else "Pictures/PairShot/$subfolder/"
                        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

            val imageUri =
                resolver.insert(imageCollection, imageDetails)
                    ?: error("MediaStore insert failed")

            try {
                copyUriToMediaStore(tempFileUri, imageUri)
                finalizePendingUri(imageUri)
                return imageUri
            } catch (e: IOException) {
                rollbackInsertedUri(imageUri)
                throw e
            } catch (e: SecurityException) {
                rollbackInsertedUri(imageUri)
                throw e
            }
        }

        private fun copyUriToMediaStore(
            sourceUri: Uri,
            destUri: Uri,
        ) {
            val resolver = context.contentResolver
            resolver.openInputStream(sourceUri)?.use { input ->
                resolver.openOutputStream(destUri)?.use { output ->
                    input.copyTo(output)
                } ?: error("Failed to open output stream")
            } ?: error("Failed to open input stream")
        }

        private fun finalizePendingUri(imageUri: Uri) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues =
                    ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }
                context.contentResolver.update(imageUri, updateValues, null, null)
            }
        }

        fun deleteFromGallery(contentUri: Uri): DeleteResult =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                deleteFromGalleryApi29(contentUri)
            } else {
                deleteFromGalleryLegacy(contentUri)
            }

        @RequiresApi(Build.VERSION_CODES.Q)
        private fun deleteFromGalleryApi29(contentUri: Uri): DeleteResult =
            try {
                val rows = context.contentResolver.delete(contentUri, null, null)
                if (rows > 0) DeleteResult.Success else DeleteResult.NotFound
            } catch (e: RecoverableSecurityException) {
                DeleteResult.RecoverablePermission(e)
            } catch (e: SecurityException) {
                DeleteResult.Failed(e)
            } catch (e: IOException) {
                DeleteResult.Failed(e)
            } catch (e: IllegalArgumentException) {
                DeleteResult.Failed(e)
            } catch (e: UnsupportedOperationException) {
                DeleteResult.Failed(e)
            }

        private fun deleteFromGalleryLegacy(contentUri: Uri): DeleteResult =
            try {
                val rows = context.contentResolver.delete(contentUri, null, null)
                if (rows > 0) DeleteResult.Success else DeleteResult.NotFound
            } catch (e: SecurityException) {
                DeleteResult.Failed(e)
            } catch (e: IOException) {
                DeleteResult.Failed(e)
            } catch (e: IllegalArgumentException) {
                DeleteResult.Failed(e)
            } catch (e: UnsupportedOperationException) {
                DeleteResult.Failed(e)
            }

        fun saveBitmapToGallery(
            bitmap: Bitmap,
            subfolder: String,
            displayName: String,
            quality: Int = 95,
        ): Uri {
            val resolver = context.contentResolver

            val imageCollection =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

            val imageDetails =
                ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val relativePath =
                            if (subfolder.isBlank()) "Pictures/PairShot/" else "Pictures/PairShot/$subfolder/"
                        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

            val imageUri =
                resolver.insert(imageCollection, imageDetails)
                    ?: error("MediaStore insert failed")

            try {
                resolver.openOutputStream(imageUri)?.use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
                } ?: error("Failed to open output stream")

                finalizePendingUri(imageUri)
                return imageUri
            } catch (e: IOException) {
                rollbackInsertedUri(imageUri)
                throw e
            } catch (e: SecurityException) {
                rollbackInsertedUri(imageUri)
                throw e
            }
        }

        private fun rollbackInsertedUri(uri: Uri) {
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (rollbackError: SecurityException) {
                Timber.w(rollbackError, "MediaStore rollback failed for $uri")
            } catch (rollbackError: IOException) {
                Timber.w(rollbackError, "MediaStore rollback failed for $uri")
            }
        }
    }
