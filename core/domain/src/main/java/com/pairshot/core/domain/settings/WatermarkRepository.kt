package com.pairshot.core.domain.settings

import com.pairshot.core.model.WatermarkConfig
import kotlinx.coroutines.flow.Flow

interface WatermarkRepository {
    val watermarkConfigFlow: Flow<WatermarkConfig>

    suspend fun saveConfig(config: WatermarkConfig)

    suspend fun getConfig(): WatermarkConfig

    suspend fun saveLogoFile(sourceUri: String): String

    suspend fun pruneOldLogoFiles(keepPath: String)

    suspend fun removeLogoFile()
}
