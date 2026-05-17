package com.pairshot.core.data.export

import android.net.Uri
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.RenderProfile
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.rendering.PairImageComposer
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatermarkedBitmapWriter
    @Inject
    constructor(
        private val pairImageComposer: PairImageComposer,
    ) {
        suspend fun combineWithWatermark(
            beforeUri: String,
            afterUri: String,
            destFile: File,
            config: WatermarkConfig,
            jpegQuality: Int,
            combineConfig: CombineConfig = CombineConfig(),
            profile: RenderProfile = RenderProfile.FULL,
        ) {
            pairImageComposer.composeToFile(
                beforeUri = Uri.parse(beforeUri),
                afterUri = Uri.parse(afterUri),
                destFile = destFile,
                combineConfig = combineConfig,
                watermarkConfig = config.copy(enabled = true),
                jpegQuality = jpegQuality,
                profile = profile,
            )
        }
    }
