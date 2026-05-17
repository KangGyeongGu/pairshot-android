package com.pairshot.core.domain.tutorial

import kotlinx.coroutines.flow.StateFlow

interface TutorialModeProvider {
    val isActive: StateFlow<Boolean>
}
