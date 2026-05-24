package com.pairshot.app.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pairshot.core.domain.settings.AppSettingsRepository
import com.pairshot.core.model.AppTextScale
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppShellViewModel
@Inject
constructor(
    appSettingsRepository: AppSettingsRepository,
) : ViewModel() {
    val textScale: StateFlow<AppTextScale> =
        appSettingsRepository.appTextScaleNameFlow
            .map(AppTextScale::fromName)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = AppTextScale.DEFAULT,
            )
}
