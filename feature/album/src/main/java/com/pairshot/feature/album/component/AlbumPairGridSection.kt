package com.pairshot.feature.album.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pairshot.core.adsui.component.PairShotNativeAdCard
import com.pairshot.core.ads.di.AdsEntryPoint
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.domain.pair.PairListItem
import com.pairshot.core.domain.pair.buildPairListWithAds
import com.pairshot.core.model.PhotoPair
import com.pairshot.core.model.SortOrder
import com.pairshot.core.ui.component.PairCard
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.map

@Composable
fun AlbumPairGridSection(
    pairs: List<PhotoPair>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    sortOrder: SortOrder,
    onPairClick: (Long) -> Unit,
    onPairLongPress: (Long) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val sortedPairs =
        remember(pairs, sortOrder) {
            when (sortOrder) {
                SortOrder.DESC -> pairs.sortedByDescending { it.beforeTimestamp }
                SortOrder.ASC -> pairs.sortedBy { it.beforeTimestamp }
            }
        }

    val context = LocalContext.current
    val entryPoint =
        remember(context) {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                AdsEntryPoint::class.java,
            )
        }
    val entitlementProvider = remember(entryPoint) { entryPoint.proEntitlementProvider() }
    val poolProvider = remember(entryPoint) { entryPoint.nativeAdPoolProvider() }
    val isActiveFlow = remember(entitlementProvider) { entitlementProvider.observe().map { it.isActive } }
    val isAdFree: Boolean? by isActiveFlow.collectAsStateWithLifecycle(initialValue = null)

    val nativeAdPool = remember(poolProvider) { poolProvider.get() }
    DisposableEffect(nativeAdPool) {
        onDispose { nativeAdPool.close() }
    }
    val nativeAds by nativeAdPool.observeAds().collectAsStateWithLifecycle()

    val items =
        remember(sortedPairs, isAdFree) {
            buildPairListWithAds(sortedPairs, isAdFree == true)
        }
    val totalAdSlots = remember(items) { items.count { it is PairListItem.Ad } }

    LaunchedEffect(totalAdSlots, isAdFree) {
        if (isAdFree == false && totalAdSlots > 0) {
            nativeAdPool.ensurePreloaded(totalAdSlots)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(PairShotSpacing.iconTextGap),
        horizontalArrangement = Arrangement.spacedBy(PairShotSpacing.iconTextGap),
    ) {
        items.forEach { entry ->
            when (entry) {
                is PairListItem.Pair -> {
                    val pair = entry.pair
                    item(
                        key = "pair_${pair.id}",
                        span = { GridItemSpan(1) },
                    ) {
                        PairCard(
                            pair = pair,
                            isSelected = pair.id in selectedIds,
                            isSelectionMode = isSelectionMode,
                            onClick = { onPairClick(pair.id) },
                            onLongPress = { onPairLongPress(pair.id) },
                        )
                    }
                }

                is PairListItem.Ad -> {
                    val slot = entry.slotIndex
                    val nativeAd = nativeAds.getOrNull(slot)
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
