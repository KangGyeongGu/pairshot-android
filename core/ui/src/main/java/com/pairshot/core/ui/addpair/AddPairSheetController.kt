package com.pairshot.core.ui.addpair

import com.pairshot.core.domain.pair.CanCreatePairUseCase
import com.pairshot.core.domain.pair.ImportPairFromGalleryUseCase
import com.pairshot.core.ui.component.AddPairDraft
import com.pairshot.core.ui.component.AddPairSlot
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AddPairOpenResult { OPENED, LIMIT_REACHED, BLOCKED }

class AddPairSheetController(
    private val canCreatePairUseCase: CanCreatePairUseCase,
    private val importPairFromGalleryUseCase: ImportPairFromGalleryUseCase,
) {
    data class State(
        val drafts: ImmutableList<AddPairDraft>,
        val isImporting: Boolean = false,
        val showSaveError: Boolean = false,
    )

    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state.asStateFlow()

    private var remaining: Int? = null
    private var nextDraftId = 0L

    suspend fun open(): Boolean {
        val result = canCreatePairUseCase()
        if (result !is CanCreatePairUseCase.Result.Allowed) return false
        remaining = result.remaining
        nextDraftId = 0L
        val initialCount = minOf(INITIAL_DRAFT_COUNT, result.remaining ?: INITIAL_DRAFT_COUNT)
        _state.value =
            State(
                drafts = List(initialCount) { AddPairDraft(id = nextDraftId++) }.toImmutableList(),
            )
        return true
    }

    fun setPhoto(
        draftId: Long,
        slot: AddPairSlot,
        uri: String,
    ) {
        val current = _state.value ?: return
        val updated =
            current.drafts.map { draft ->
                if (draft.id != draftId) {
                    draft
                } else {
                    when (slot) {
                        AddPairSlot.BEFORE -> draft.copy(beforeUri = uri)
                        AddPairSlot.AFTER -> draft.copy(afterUri = uri)
                    }
                }
            }
        val withEmpty =
            if (updated.last().beforeUri != null && updated.size < (remaining ?: Int.MAX_VALUE)) {
                updated + AddPairDraft(id = nextDraftId++)
            } else {
                updated
            }
        _state.value = current.copy(drafts = withEmpty.toImmutableList(), showSaveError = false)
    }

    suspend fun confirm(albumId: Long? = null) {
        val current = _state.value ?: return
        val filled = current.drafts.filter { it.beforeUri != null }
        if (filled.isEmpty()) return
        _state.value = current.copy(isImporting = true, showSaveError = false)
        val createdIds = mutableSetOf<Long>()
        for (draft in filled) {
            try {
                importPairFromGalleryUseCase(
                    beforeUri = requireNotNull(draft.beforeUri),
                    afterUri = draft.afterUri,
                    albumId = albumId,
                )
                createdIds.add(draft.id)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                val remainingDrafts = current.drafts.filterNot { it.id in createdIds }
                _state.value =
                    current.copy(
                        drafts = remainingDrafts.toImmutableList(),
                        isImporting = false,
                        showSaveError = true,
                    )
                return
            }
        }
        _state.value = null
    }

    fun dismiss() {
        _state.value = null
    }

    private companion object {
        const val INITIAL_DRAFT_COUNT = 2
    }
}
