package com.pairshot.feature.home.component

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pairshot.core.ads.di.AdsEntryPoint
import com.pairshot.core.adsui.component.PairShotNativeAdCard
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.domain.pair.PairListItem
import com.pairshot.core.domain.pair.buildPairListWithAds
import com.pairshot.core.domain.tutorial.AnchorKey
import com.pairshot.core.model.PhotoPair
import com.pairshot.core.model.SortOrder
import com.pairshot.core.ui.component.PairCard
import com.pairshot.feature.home.R
import com.pairshot.feature.tutorial.ui.modifier.tutorialAnchor
import dagger.hilt.android.EntryPointAccessors
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy. MM. dd", Locale.KOREAN)

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
private fun formatDateLabel(
    date: LocalDate,
    today: LocalDate,
): String {
    val base = date.format(DateFormatter)
    return when (date) {
        today -> stringResource(R.string.home_date_suffix_today, base)
        today.minusDays(1) -> stringResource(R.string.home_date_suffix_yesterday, base)
        else -> base
    }
}

@Composable
fun HomePairGridSection(
    pairs: ImmutableList<PhotoPair>,
    selectedIds: ImmutableSet<Long>,
    selectionMode: Boolean,
    sortOrder: SortOrder,
    onPairClick: (Long) -> Unit,
    onPairLongClick: (Long) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now(ZoneId.systemDefault()) }

    val sortedPairs =
        remember(pairs, sortOrder) {
            when (sortOrder) {
                SortOrder.DESC -> pairs.sortedByDescending { it.beforeTimestamp }
                SortOrder.ASC -> pairs.sortedBy { it.beforeTimestamp }
            }
        }

    val isInspection = LocalInspectionMode.current
    val context = LocalContext.current
    val entryPoint =
        if (isInspection) {
            null
        } else {
            remember(context) {
                EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    AdsEntryPoint::class.java,
                )
            }
        }
    val membershipProvider = remember(entryPoint) { entryPoint?.membershipProvider() }
    val poolProvider = remember(entryPoint) { entryPoint?.nativeAdPoolProvider() }
    val adFreeFlow = remember(membershipProvider) { membershipProvider?.observe()?.map { it.isAdFree } }

    val isAdFree: Boolean? =
        if (isInspection) {
            true
        } else {
            adFreeFlow?.collectAsStateWithLifecycle(initialValue = null)?.value
        }

    val nativeAdPool = remember(poolProvider) { poolProvider?.get() }
    DisposableEffect(nativeAdPool) {
        onDispose { nativeAdPool?.close() }
    }
    val nativeAds: List<com.google.android.gms.ads.nativead.NativeAd> =
        nativeAdPool?.observeAds()?.collectAsStateWithLifecycle()?.value ?: emptyList()

    val items =
        remember(sortedPairs, isAdFree) {
            buildPairListWithAds(
                pairs = sortedPairs,
                adFree = isAdFree == true,
                sectionKeyOf = { it.beforeTimestamp.toLocalDate() },
            )
        }
    val totalAdSlots = remember(items) { items.count { it is PairListItem.Ad } }
    val firstPairId =
        remember(items) {
            items
                .filterIsInstance<PairListItem.Pair>()
                .firstOrNull()
                ?.pair
                ?.id
        }

    LaunchedEffect(totalAdSlots, isAdFree) {
        if (isAdFree == false && totalAdSlots > 0) {
            nativeAdPool?.ensurePreloaded(totalAdSlots)
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
                                text = formatDateLabel(pairDate, today),
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
                            if (pair.id == firstPairId) {
                                Modifier.tutorialAnchor(AnchorKey.HOME_PAIR_CARD_FIRST)
                            } else {
                                Modifier
                            }
                        androidx.compose.foundation.layout
                            .Box(modifier = anchorModifier) {
                                PairCard(
                                    pair = pair,
                                    isSelected = pair.id in selectedIds,
                                    isSelectionMode = selectionMode,
                                    onClick = { onPairClick(pair.id) },
                                    onLongPress = { onPairLongClick(pair.id) },
                                )
                            }
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
