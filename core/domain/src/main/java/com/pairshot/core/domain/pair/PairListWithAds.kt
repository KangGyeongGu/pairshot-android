package com.pairshot.core.domain.pair

import com.pairshot.core.model.PhotoPair

private const val GRID_COLUMNS = 2
private const val ROWS_PER_AD = 2
private const val MIN_PAIRS_FOR_ADS = 5

fun buildPairListWithAds(
    pairs: List<PhotoPair>,
    isPro: Boolean,
    sectionKeyOf: (PhotoPair) -> Any? = { null },
): List<PairListItem> {
    if (isPro || pairs.size < MIN_PAIRS_FOR_ADS) {
        return pairs.map { PairListItem.Pair(it) }
    }

    val result = mutableListOf<PairListItem>()
    var adSlotIndex = 0
    var rowsSinceLastAd = 0
    var pairsInCurrentRow = 0
    var lastSection: Any? = null
    var sectionInitialized = false

    pairs.forEachIndexed { index, pair ->
        val section = sectionKeyOf(pair)
        if (sectionInitialized && section != lastSection && pairsInCurrentRow > 0) {
            pairsInCurrentRow = 0
            rowsSinceLastAd += 1
            if (rowsSinceLastAd >= ROWS_PER_AD) {
                result.add(PairListItem.Ad(adSlotIndex))
                adSlotIndex += 1
                rowsSinceLastAd = 0
            }
        }
        lastSection = section
        sectionInitialized = true

        result.add(PairListItem.Pair(pair))
        pairsInCurrentRow += 1
        if (pairsInCurrentRow == GRID_COLUMNS) {
            pairsInCurrentRow = 0
            rowsSinceLastAd += 1
        }

        val hasMoreAfter = index < pairs.lastIndex
        if (pairsInCurrentRow == 0 && rowsSinceLastAd >= ROWS_PER_AD && hasMoreAfter) {
            result.add(PairListItem.Ad(adSlotIndex))
            adSlotIndex += 1
            rowsSinceLastAd = 0
        }
    }
    return result
}
