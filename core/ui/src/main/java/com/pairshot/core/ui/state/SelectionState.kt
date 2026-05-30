package com.pairshot.core.ui.state

import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet

data class SelectionState(
    val selectedIds: ImmutableSet<Long> = persistentSetOf(),
    val isSelectionMode: Boolean = false,
) {
    val selectedCount: Int get() = selectedIds.size

    val hasSelection: Boolean get() = selectedIds.isNotEmpty()

    fun toggle(id: Long): SelectionState {
        val next =
            if (id in selectedIds) selectedIds - id else selectedIds + id
        return copy(
            selectedIds = next.toPersistentSet(),
            isSelectionMode = next.isNotEmpty(),
        )
    }

    fun enterWith(initialId: Long): SelectionState =
        SelectionState(
            selectedIds = persistentSetOf(initialId),
            isSelectionMode = true,
        )

    fun cleared(): SelectionState = SelectionState()

    fun replaceIds(ids: Set<Long>): SelectionState =
        copy(selectedIds = ids.toPersistentSet())
}
