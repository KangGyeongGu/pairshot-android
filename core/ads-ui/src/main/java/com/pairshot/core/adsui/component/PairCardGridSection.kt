package com.pairshot.core.adsui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pairshot.core.ads.controller.NativeAdPool
import com.pairshot.core.ads.di.AdsEntryPoint
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.domain.pair.PairListItem
import com.pairshot.core.domain.pair.buildPairListWithAds
import com.pairshot.core.model.PhotoPair
import com.pairshot.core.model.SortOrder
import com.pairshot.core.ui.component.PairCard
import dagger.hilt.android.EntryPointAccessors
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DefaultDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy. MM. dd", Locale.KOREAN)

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
fun PairCardGridSection(
    pairs: ImmutableList<PhotoPair>,
    selectedIds: ImmutableSet<Long>,
    isSelectionMode: Boolean,
    sortOrder: SortOrder,
    onPairClick: (Long) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    firstPairModifier: Modifier = Modifier,
    onPairLongPress: (Long) -> Unit = {},
    showAds: Boolean = true,
    disabledIds: ImmutableSet<Long> = persistentSetOf(),
    disabledLabel: String? = null,
    dateHeaderLabel: @Composable (LocalDate) -> String = { it.format(DefaultDateFormatter) },
) {
    val sortedPairs =
        remember(pairs, sortOrder) {
            when (sortOrder) {
                SortOrder.DESC -> pairs.sortedByDescending { it.beforeTimestamp }
                SortOrder.ASC -> pairs.sortedBy { it.beforeTimestamp }
            }
        }

    val adContext = rememberAdContext(enabled = showAds)
    val firstPairId =
        remember(sortedPairs) { sortedPairs.firstOrNull()?.id }

    val items =
        remember(sortedPairs, adContext.isAdFree) {
            buildPairListWithAds(
                pairs = sortedPairs,
                adFree = adContext.isAdFree == true,
                sectionKeyOf = { it.beforeTimestamp.toLocalDate() },
            )
        }
    val totalAdSlots = remember(items) { items.count { it is PairListItem.Ad } }

    LaunchedEffect(totalAdSlots, adContext.isAdFree) {
        if (adContext.isAdFree == false && totalAdSlots > 0) {
            adContext.pool?.ensurePreloaded(totalAdSlots)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(PairShotSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(PairShotSpacing.sm),
    ) {
        var lastDate: LocalDate? = null
        items.forEach { entry ->
            when (entry) {
                is PairListItem.Pair -> {
                    val pair = entry.pair
                    val pairDate = pair.beforeTimestamp.toLocalDate()
                    if (pairDate != lastDate) {
                        lastDate = pairDate
                        item(
                            key = "header_$pairDate",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            Text(
                                text = dateHeaderLabel(pairDate),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = PairShotSpacing.sm,
                                        bottom = PairShotSpacing.xs,
                                    ),
                            )
                        }
                    }
                    item(
                        key = "pair_${pair.id}",
                        span = { GridItemSpan(1) },
                    ) {
                        val anchorModifier =
                            if (pair.id == firstPairId) firstPairModifier else Modifier
                        Box(modifier = anchorModifier) {
                            PairCard(
                                pair = pair,
                                isSelected = pair.id in selectedIds,
                                isSelectionMode = isSelectionMode,
                                isDisabled = pair.id in disabledIds,
                                disabledLabel = disabledLabel,
                                onClick = { onPairClick(pair.id) },
                                onLongPress = { onPairLongPress(pair.id) },
                            )
                        }
                    }
                }

                is PairListItem.Ad -> {
                    val slot = entry.slotIndex
                    val nativeAd = adContext.nativeAds.getOrNull(slot)
                    if (nativeAd != null) {
                        item(
                            key = "ad_$slot",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            PairShotNativeAdCard(nativeAd = nativeAd)
                        }
                    }
                }
            }
        }
    }
}

private data class AdContext(
    val isAdFree: Boolean?,
    val pool: NativeAdPool?,
    val nativeAds: List<com.google.android.gms.ads.nativead.NativeAd>,
)

@Composable
private fun rememberAdContext(enabled: Boolean): AdContext {
    val isInspection = LocalInspectionMode.current
    if (!enabled || isInspection) {
        return AdContext(isAdFree = true, pool = null, nativeAds = emptyList())
    }

    val context = LocalContext.current
    val entryPoint =
        remember(context) {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                AdsEntryPoint::class.java,
            )
        }
    val membershipProvider = remember(entryPoint) { entryPoint.membershipProvider() }
    val poolProvider = remember(entryPoint) { entryPoint.nativeAdPoolProvider() }
    val adFreeFlow = remember(membershipProvider) { membershipProvider.observe().map { it.isAdFree } }
    val isAdFree: Boolean? by adFreeFlow.collectAsStateWithLifecycle(initialValue = null)

    val nativeAdPool = remember(poolProvider) { poolProvider.get() }
    DisposableEffect(nativeAdPool) {
        onDispose { nativeAdPool.close() }
    }
    val nativeAds by nativeAdPool.observeAds().collectAsStateWithLifecycle()

    return AdContext(isAdFree = isAdFree, pool = nativeAdPool, nativeAds = nativeAds)
}
