package com.pairshot.core.domain.export

sealed interface ExportAction {
    data class ShareImages(
        val uris: List<String>,
    ) : ExportAction

    data class ShareZip(
        val filePath: String,
    ) : ExportAction
}

sealed interface SaveToDeviceResult {
    data class SavedImagesToGallery(
        val count: Int,
    ) : SaveToDeviceResult

    data class ZipReadyForSave(
        val filePath: String,
        val suggestedName: String,
    ) : SaveToDeviceResult

    data object Nothing : SaveToDeviceResult
}
