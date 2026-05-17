package com.pairshot.feature.tutorial.domain

import com.pairshot.core.domain.tutorial.AnchorBounds
import com.pairshot.core.domain.tutorial.AnchorKey
import com.pairshot.core.domain.tutorial.TutorialAnchorReporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TutorialAnchorRegistry
    @Inject
    constructor() : TutorialAnchorReporter {
        private val _bounds = MutableStateFlow<Map<AnchorKey, AnchorBounds>>(emptyMap())
        val bounds: StateFlow<Map<AnchorKey, AnchorBounds>> = _bounds.asStateFlow()

        override fun report(
            key: AnchorKey,
            bounds: AnchorBounds?,
        ) {
            val current = _bounds.value
            val existing = current[key]
            if (bounds == null) {
                if (existing == null) return
                _bounds.value = current - key
            } else {
                if (existing == bounds) return
                _bounds.value = current + (key to bounds)
            }
        }
    }
