package com.pairshot.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photo_pairs")
data class PhotoPairEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val beforePhotoUri: String? = null,
    val afterPhotoUri: String? = null,
    val beforeTimestamp: Long = System.currentTimeMillis(),
    val afterTimestamp: Long? = null,
    val status: String = "BEFORE_ONLY",
    val zoomLevel: Float? = null,
    val aspectRatio: String? = null,
)
