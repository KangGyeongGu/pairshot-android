package com.pairshot.core.domain.combine

import com.pairshot.core.model.CombineConfig
import kotlinx.coroutines.flow.Flow

interface CombineSettingsRepository {
    val configFlow: Flow<CombineConfig>

    suspend fun saveConfig(config: CombineConfig)

    suspend fun getConfig(): CombineConfig
}
