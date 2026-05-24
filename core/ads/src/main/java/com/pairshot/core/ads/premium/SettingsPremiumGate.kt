package com.pairshot.core.ads.premium

import com.pairshot.core.domain.premium.PremiumFeature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsPremiumGate
@Inject
constructor() {
    private val unlocked = MutableStateFlow<Set<PremiumFeature>>(emptySet())

    fun isUnlocked(feature: PremiumFeature): Boolean = unlocked.value.contains(feature)

    fun unlock(feature: PremiumFeature) {
        unlocked.update { it + feature }
    }
}
