package com.pairshot.core.domain.export

interface ExportRepository {
    suspend fun executeShare(
        plan: ExportPlan,
        onProgress: (ExportProgress) -> Unit,
    ): ExportAction

    suspend fun executeSaveToDevice(
        plan: ExportPlan,
        onProgress: (ExportProgress) -> Unit,
    ): SaveToDeviceResult

    suspend fun discardPreparedZip(filePath: String)
}
