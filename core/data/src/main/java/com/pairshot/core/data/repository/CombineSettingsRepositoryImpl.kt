package com.pairshot.core.data.repository

import com.pairshot.core.datastore.CombinePreferences
import com.pairshot.core.domain.combine.CombineSettingsRepository
import com.pairshot.core.model.CombineConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CombineSettingsRepositoryImpl
@Inject
constructor(
    private val combinePreferences: CombinePreferences,
) : CombineSettingsRepository {
    override val configFlow: Flow<CombineConfig> = combinePreferences.configFlow

    override suspend fun saveConfig(config: CombineConfig) {
        combinePreferences.saveConfig(config)
    }

    override suspend fun getConfig(): CombineConfig = combinePreferences.configFlow.first()
}
