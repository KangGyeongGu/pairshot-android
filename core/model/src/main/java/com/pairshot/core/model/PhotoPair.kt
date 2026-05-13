package com.pairshot.core.model

data class PhotoPair(
    val id: Long = 0,
    val beforePhotoUri: String? = null,
    val afterPhotoUri: String? = null,
    val beforeTimestamp: Long,
    val afterTimestamp: Long? = null,
    val status: PairStatus = PairStatus.BEFORE_ONLY,
    val zoomLevel: Float? = null,
    val hasCombined: Boolean = false,
    val aspectRatio: AspectRatio? = null,
)
