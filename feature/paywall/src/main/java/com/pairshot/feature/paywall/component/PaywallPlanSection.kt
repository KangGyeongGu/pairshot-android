package com.pairshot.feature.paywall.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.pairshot.core.billing.domain.BillingOffer
import com.pairshot.core.designsystem.PairShotRadius
import com.pairshot.core.designsystem.PairShotScreen
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotStroke
import com.pairshot.feature.paywall.PaywallUiState
import com.pairshot.feature.paywall.R
import java.text.NumberFormat
import java.util.Currency
import kotlin.math.roundToInt

private val PLAN_CARD_CORNER = PairShotSpacing.lg
private val PLAN_CARD_PADDING = PairShotScreen.horizontalPadding
private const val MONTHS_PER_YEAR = 12L
private const val MICROS_PER_UNIT = 1_000_000.0
private const val PERCENT_SCALE = 100.0
private const val BADGE_FEATURED_BG_ALPHA = 0.22f
private const val PLAN_BORDER_ALPHA = 0.4f
private const val PLAN_SUBTITLE_ALPHA = 0.78f

@Composable
internal fun PaywallPlanSection(
    state: PaywallUiState,
    onStartTrial: (BillingOffer) -> Unit,
    onPurchaseYearly: (BillingOffer) -> Unit,
    onPurchaseMonthly: (BillingOffer) -> Unit,
    onRetryLoad: () -> Unit,
) {
    when {
        state.loading -> {
            LoadingBlock()
        }

        state.loadError -> {
            ErrorBlock(onRetryLoad = onRetryLoad)
        }

        else -> {
            val yearly = state.yearlyOffer
            val trial = state.trialOffer
            val monthlyBase = state.monthlyOffer

            if (yearly != null) {
                val discount = discountPercent(monthlyBase, yearly)
                PlanCard(
                    title = stringResource(R.string.paywall_plan_yearly_title),
                    price = stringResource(R.string.paywall_plan_yearly_price, yearly.priceFormatted),
                    subtitle =
                    stringResource(
                        R.string.paywall_plan_yearly_monthly_equivalent,
                        monthlyEquivalent(yearly),
                    ),
                    badge = discount?.let { stringResource(R.string.paywall_plan_yearly_badge, it) },
                    featured = true,
                    onClick = { onPurchaseYearly(yearly) },
                )
                Spacer(modifier = Modifier.height(PairShotSpacing.md))
            }

            if (monthlyBase != null) {
                PlanCard(
                    title = stringResource(R.string.paywall_plan_monthly_title),
                    price = stringResource(R.string.paywall_plan_monthly_price, monthlyBase.priceFormatted),
                    subtitle = if (trial != null) stringResource(R.string.paywall_plan_monthly_trial_note) else "",
                    badge = null,
                    featured = false,
                    onClick = {
                        if (trial != null) onStartTrial(trial) else onPurchaseMonthly(monthlyBase)
                    },
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    subtitle: String,
    badge: String?,
    featured: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (featured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (featured) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val border =
        if (featured) {
            null
        } else {
            BorderStroke(PairShotStroke.hairline, MaterialTheme.colorScheme.outline.copy(alpha = PLAN_BORDER_ALPHA))
        }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PLAN_CARD_CORNER),
        color = bg,
        contentColor = fg,
        border = border,
    ) {
        Column(modifier = Modifier.padding(PLAN_CARD_PADDING)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (badge != null) {
                    Spacer(modifier = Modifier.width(PairShotSpacing.md))
                    Badge(text = badge, featured = featured)
                }
            }
            if (price.isNotEmpty()) {
                Spacer(modifier = Modifier.height(PairShotRadius.sm))
                Text(
                    text = price,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(if (price.isEmpty()) PairShotRadius.sm else PairShotStroke.thin))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = fg.copy(alpha = PLAN_SUBTITLE_ALPHA),
                )
            }
        }
    }
}

@Composable
private fun Badge(
    text: String,
    featured: Boolean,
) {
    val badgeBg =
        if (featured) {
            Color.White.copy(alpha = BADGE_FEATURED_BG_ALPHA)
        } else {
            MaterialTheme.colorScheme.primary
        }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(PairShotRadius.sm))
            .background(badgeBg)
            .padding(horizontal = PairShotSpacing.sm, vertical = PairShotStroke.thick),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LoadingBlock() {
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = PairShotSpacing.xxxl * 3),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(PairShotSpacing.sm))
            Text(
                text = stringResource(R.string.paywall_loading),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ErrorBlock(onRetryLoad: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = PairShotSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.paywall_load_failed),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(PairShotSpacing.md))
        OutlinedButton(onClick = onRetryLoad) {
            Text(text = stringResource(R.string.paywall_load_failed_retry))
        }
    }
}

private fun monthlyEquivalent(yearly: BillingOffer): String {
    val perMonthMicros = yearly.priceAmountMicros / MONTHS_PER_YEAR
    val perMonthValue = perMonthMicros / MICROS_PER_UNIT
    val fmt = NumberFormat.getCurrencyInstance()
    runCatching { fmt.currency = Currency.getInstance(yearly.priceCurrencyCode) }
    fmt.maximumFractionDigits = 0
    return fmt.format(perMonthValue)
}

private fun discountPercent(
    monthly: BillingOffer?,
    yearly: BillingOffer?,
): Int? {
    if (monthly == null || yearly == null || monthly.priceAmountMicros <= 0L) return null
    val yearlyFullPriceMicros = monthly.priceAmountMicros * MONTHS_PER_YEAR
    if (yearly.priceAmountMicros >= yearlyFullPriceMicros) return null
    val ratio = yearly.priceAmountMicros.toDouble() / yearlyFullPriceMicros.toDouble()
    val percent = ((1.0 - ratio) * PERCENT_SCALE).roundToInt()
    return percent.takeIf { it > 0 }
}
