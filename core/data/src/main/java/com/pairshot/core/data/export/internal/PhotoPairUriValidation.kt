package com.pairshot.core.data.export.internal

import android.net.Uri
import com.pairshot.core.database.entity.PhotoPairEntity

internal fun PhotoPairEntity.validBeforeUriOrNull(): String? =
    beforePhotoUri?.takeIf {
        it.isNotBlank() && Uri.parse(it).scheme == "content"
    }

internal fun PhotoPairEntity.validAfterUriOrNull(): String? =
    afterPhotoUri?.takeIf {
        it.isNotBlank() && Uri.parse(it).scheme == "content"
    }
