package com.pairshot.feature.tutorial.domain

import com.pairshot.core.datastore.AppPreferences
import com.pairshot.core.domain.tutorial.TutorialActionDispatcher
import com.pairshot.core.domain.tutorial.TutorialModeProvider
import com.pairshot.core.domain.tutorial.TutorialReplayController
import com.pairshot.feature.tutorial.internal.TutorialSandbox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

enum class TutorialFinishReason { FIRST_RUN, REPLAY }

data class TutorialFinishedEvent(
    val section: TutorialSection,
    val reason: TutorialFinishReason,
)

@Singleton
class TutorialCoordinator
    @Inject
    constructor(
        private val appPreferences: AppPreferences,
        private val sandbox: TutorialSandbox,
    ) : TutorialModeProvider,
        TutorialActionDispatcher,
        TutorialReplayController {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        private val _currentStep = MutableStateFlow<TutorialStepId?>(null)
        val currentStep: StateFlow<TutorialStepId?> = _currentStep.asStateFlow()

        private var currentSection: TutorialSection = TutorialSection.MAIN_ONBOARDING
        private var currentRunReason: TutorialFinishReason = TutorialFinishReason.FIRST_RUN

        private val _finishedEvents =
            MutableSharedFlow<TutorialFinishedEvent>(
                replay = 0,
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        val finishedEvents: SharedFlow<TutorialFinishedEvent> = _finishedEvents.asSharedFlow()

        override val isActive: StateFlow<Boolean> =
            _currentStep
                .map { it != null && it != TutorialStepId.DONE }
                .stateIn(scope, SharingStarted.Eagerly, false)

        fun start(section: TutorialSection = TutorialSection.MAIN_ONBOARDING) {
            if (_currentStep.value != null && _currentStep.value != TutorialStepId.DONE) return
            currentSection = section
            currentRunReason = TutorialFinishReason.FIRST_RUN
            _currentStep.value = TutorialStepDefinitions.firstStepOf(section)
        }

        override fun restart() {
            currentSection = TutorialSection.MAIN_ONBOARDING
            currentRunReason = TutorialFinishReason.REPLAY
            _currentStep.value = TutorialStepDefinitions.firstStepOf(TutorialSection.MAIN_ONBOARDING)
        }

        fun finish() {
            val section = currentSection
            val reason = currentRunReason
            scope.launch { finalizeSection(section, reason) }
        }

        fun skip() = finish()

        override fun report(actionId: String) {
            val currentId = _currentStep.value ?: return
            if (currentId == TutorialStepId.DONE) return
            val def = TutorialStepDefinitions.get(currentId) ?: return
            val advance = def.advance
            if (advance is AdvanceCondition.UserAction && advance.actionId == actionId) {
                advanceInternal(currentId)
            }
        }

        fun onTapAnywhere() {
            val currentId = _currentStep.value ?: return
            if (currentId == TutorialStepId.DONE) return
            val def = TutorialStepDefinitions.get(currentId) ?: return
            if (def.advance is AdvanceCondition.TapAnywhere) {
                advanceInternal(currentId)
            }
        }

        fun advanceManual() {
            val currentId = _currentStep.value ?: return
            if (currentId == TutorialStepId.DONE) return
            advanceInternal(currentId)
        }

        private fun advanceInternal(from: TutorialStepId) {
            val next = TutorialStepDefinitions.next(from)
            Timber.tag(TAG).d("advance: %s -> %s", from, next)
            if (next == TutorialStepId.DONE) {
                val section = currentSection
                val reason = currentRunReason
                scope.launch { finalizeSection(section, reason) }
            } else {
                _currentStep.value = next
            }
        }

        private suspend fun finalizeSection(
            section: TutorialSection,
            reason: TutorialFinishReason,
        ) {
            markSectionCompleted(section)
            if (section == TutorialSection.MAIN_ONBOARDING) sandbox.cleanup()
            _currentStep.value = TutorialStepId.DONE
            _finishedEvents.tryEmit(TutorialFinishedEvent(section, reason))
        }

        private suspend fun markSectionCompleted(section: TutorialSection) {
            when (section) {
                TutorialSection.MAIN_ONBOARDING -> appPreferences.setTutorialCompleted(true)
                TutorialSection.EXPORT_SETTINGS_INTRO -> appPreferences.setExportSettingsTutorialCompleted(true)
            }
        }

        companion object {
            private const val TAG = "Tutorial"
        }
    }
