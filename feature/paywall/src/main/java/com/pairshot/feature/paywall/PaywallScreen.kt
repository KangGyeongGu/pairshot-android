package com.pairshot.feature.paywall

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pairshot.core.billing.domain.BillingOffer
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotTheme
import com.pairshot.core.ui.component.PairShotSnackbarController
import com.pairshot.core.ui.component.PairShotSnackbarHost
import java.text.NumberFormat
import java.util.Currency

private val LOGO_SIZE = 56.dp
private val LOGO_CORNER = 12.dp
private val LOGO_SHADOW_ELEVATION = 10.dp
private val PLAN_CARD_CORNER = 16.dp
private val PLAN_CARD_PADDING = 20.dp
private const val LOGO_SHADOW_AMBIENT_ALPHA = 0.18f
private const val LOGO_SHADOW_SPOT_ALPHA = 0.28f
private const val MONTHS_PER_YEAR = 12L
private const val MICROS_PER_UNIT = 1_000_000.0
private const val CONTINUE_FREE_ALPHA = 0.9f
private const val LEGAL_ALPHA = 0.65f
private const val DISCLOSURE_ALPHA = 0.6f
private const val PREVIEW_MONTHLY_MICROS = 4_500_000_000L
private const val PREVIEW_YEARLY_MICROS = 43_200_000_000L
private const val PREVIEW_TRIAL_DAYS = 14

@Composable
fun PaywallScreen(
    state: PaywallUiState,
    dismissible: Boolean,
    onDismiss: () -> Unit,
    onStartTrial: (BillingOffer) -> Unit,
    onPurchaseYearly: (BillingOffer) -> Unit,
    onPurchaseMonthly: (BillingOffer) -> Unit,
    onContinueFree: () -> Unit,
    onRestore: () -> Unit,
    onRetryLoad: () -> Unit,
    termsUrl: String,
    privacyUrl: String,
    snackbarController: PairShotSnackbarController = remember { PairShotSnackbarController() },
) {
    val uriHandler = LocalUriHandler.current
    if (!dismissible) BackHandler { }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            TopBar(dismissible = dismissible, onDismiss = onDismiss)

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Hero()

                Spacer(modifier = Modifier.height(44.dp))
                ValueProp(stringResource(R.string.paywall_value_unlimited))
                Spacer(modifier = Modifier.height(14.dp))
                ValueProp(stringResource(R.string.paywall_value_no_ads))
                Spacer(modifier = Modifier.height(14.dp))
                ValueProp(stringResource(R.string.paywall_value_pro_features))

                Spacer(modifier = Modifier.height(44.dp))
                PlanSection(
                    state = state,
                    onStartTrial = onStartTrial,
                    onPurchaseYearly = onPurchaseYearly,
                    onPurchaseMonthly = onPurchaseMonthly,
                    onRetryLoad = onRetryLoad,
                )

                Spacer(modifier = Modifier.height(20.dp))
                if (!dismissible) {
                    CenteredTextLink(
                        text = stringResource(R.string.paywall_continue_free),
                        onClick = onContinueFree,
                        alpha = CONTINUE_FREE_ALPHA,
                    )
                }
                CenteredTextLink(
                    text = stringResource(R.string.paywall_restore),
                    onClick = onRestore,
                )

                Spacer(modifier = Modifier.height(20.dp))
                DisclosureBlock()
                Spacer(modifier = Modifier.height(16.dp))
            }

            LegalRow(
                modifier = Modifier.padding(horizontal = 24.dp),
                onOpenTerms = { uriHandler.openUri(termsUrl) },
                onOpenPrivacy = { uriHandler.openUri(privacyUrl) },
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        PairShotSnackbarHost(
            controller = snackbarController,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = PairShotSpacing.snackbarTopOffset),
        )
    }
}

@Composable
private fun DisclosureBlock() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DISCLOSURE_ALPHA)
    val style = MaterialTheme.typography.labelSmall
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.paywall_disclosure_auto_renew),
            style = style,
            color = color,
        )
        Text(
            text = stringResource(R.string.paywall_disclosure_withdrawal),
            style = style,
            color = color,
        )
        Text(
            text = stringResource(R.string.paywall_disclosure_minor),
            style = style,
            color = color,
        )
    }
}

@Composable
private fun TopBar(
    dismissible: Boolean,
    onDismiss: () -> Unit,
) {
    if (dismissible) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 4.dp),
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.paywall_close_content_description),
                )
            }
        }
    } else {
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun Hero() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_paywall_logo),
            contentDescription = stringResource(R.string.paywall_logo_content_description),
            modifier =
                Modifier
                    .size(LOGO_SIZE)
                    .shadow(
                        elevation = LOGO_SHADOW_ELEVATION,
                        shape = RoundedCornerShape(LOGO_CORNER),
                        ambientColor = Color.Black.copy(alpha = LOGO_SHADOW_AMBIENT_ALPHA),
                        spotColor = Color.Black.copy(alpha = LOGO_SHADOW_SPOT_ALPHA),
                    ),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.paywall_title),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.paywall_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ValueProp(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PlanSection(
    state: PaywallUiState,
    onStartTrial: (BillingOffer) -> Unit,
    onPurchaseYearly: (BillingOffer) -> Unit,
    onPurchaseMonthly: (BillingOffer) -> Unit,
    onRetryLoad: () -> Unit,
) {
    when {
        state.loading -> LoadingBlock()
        state.loadError -> ErrorBlock(onRetryLoad = onRetryLoad)
        else -> {
            val yearly = state.yearlyOffer
            val trial = state.trialOffer
            val monthlyBase = state.monthlyOffer

            if (yearly != null) {
                PlanCard(
                    title = stringResource(R.string.paywall_plan_yearly_title),
                    price = stringResource(R.string.paywall_plan_yearly_price, yearly.priceFormatted),
                    subtitle = stringResource(R.string.paywall_plan_yearly_subtitle, monthlyEquivalent(yearly)),
                    badge = stringResource(R.string.paywall_plan_yearly_badge),
                    featured = true,
                    onClick = { onPurchaseYearly(yearly) },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (monthlyBase != null) {
                val monthlySubtitle =
                    if (trial != null) stringResource(R.string.paywall_plan_monthly_trial_note) else ""
                PlanCard(
                    title = stringResource(R.string.paywall_plan_monthly_title),
                    price = stringResource(R.string.paywall_plan_monthly_price, monthlyBase.priceFormatted),
                    subtitle = monthlySubtitle,
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
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
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
                    Spacer(modifier = Modifier.width(10.dp))
                    Badge(text = badge, featured = featured)
                }
            }
            if (price.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = price,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(if (price.isEmpty()) 6.dp else 2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = fg.copy(alpha = 0.78f),
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
            Color.White.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.primary
        }
    val badgeFg =
        if (featured) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onPrimary
        }
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(badgeBg)
                .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = badgeFg,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LoadingBlock() {
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.paywall_load_failed),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onRetryLoad) {
            Text(text = stringResource(R.string.paywall_load_failed_retry))
        }
    }
}

@Composable
private fun CenteredTextLink(
    text: String,
    onClick: () -> Unit,
    alpha: Float = 1f,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
        textAlign = TextAlign.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
    )
}

@Composable
private fun LegalRow(
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.paywall_terms),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = LEGAL_ALPHA),
            modifier = Modifier.clickable(onClick = onOpenTerms).padding(8.dp),
        )
        Text(
            text = "·",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = LEGAL_ALPHA),
        )
        Text(
            text = stringResource(R.string.paywall_privacy),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = LEGAL_ALPHA),
            modifier = Modifier.clickable(onClick = onOpenPrivacy).padding(8.dp),
        )
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

private fun sampleOffer(
    plan: String,
    price: String,
    micros: Long,
    trial: Int? = null,
): BillingOffer =
    BillingOffer(
        productId = "pairshot_pro",
        basePlanId = plan,
        offerId = if (trial != null) "trial14" else null,
        offerToken = "preview-token",
        priceFormatted = price,
        priceAmountMicros = micros,
        priceCurrencyCode = "KRW",
        billingPeriodIso = if (plan == "yearly") "P1Y" else "P1M",
        trialDays = trial,
    )

@Preview(name = "Paywall — Hard (first launch)", showBackground = true, heightDp = 800)
@Composable
@Suppress("UnusedPrivateMember")
private fun PreviewPaywallHard() {
    PairShotTheme {
        PaywallScreen(
            state =
                PaywallUiState(
                    loading = false,
                    trialOffer = sampleOffer("monthly", "₩4,500", PREVIEW_MONTHLY_MICROS, trial = PREVIEW_TRIAL_DAYS),
                    monthlyOffer = sampleOffer("monthly", "₩4,500", PREVIEW_MONTHLY_MICROS),
                    yearlyOffer = sampleOffer("yearly", "₩43,200", PREVIEW_YEARLY_MICROS),
                ),
            dismissible = false,
            onDismiss = {},
            onStartTrial = {},
            onPurchaseYearly = {},
            onPurchaseMonthly = {},
            onContinueFree = {},
            onRestore = {},
            onRetryLoad = {},
            termsUrl = "",
            privacyUrl = "",
        )
    }
}

@Preview(name = "Paywall — Dismissible", showBackground = true, heightDp = 800)
@Composable
@Suppress("UnusedPrivateMember")
private fun PreviewPaywallDismissible() {
    PairShotTheme {
        PaywallScreen(
            state =
                PaywallUiState(
                    loading = false,
                    trialOffer = sampleOffer("monthly", "₩4,500", PREVIEW_MONTHLY_MICROS, trial = PREVIEW_TRIAL_DAYS),
                    monthlyOffer = sampleOffer("monthly", "₩4,500", PREVIEW_MONTHLY_MICROS),
                    yearlyOffer = sampleOffer("yearly", "₩43,200", PREVIEW_YEARLY_MICROS),
                ),
            dismissible = true,
            onDismiss = {},
            onStartTrial = {},
            onPurchaseYearly = {},
            onPurchaseMonthly = {},
            onContinueFree = {},
            onRestore = {},
            onRetryLoad = {},
            termsUrl = "",
            privacyUrl = "",
        )
    }
}

@Preview(name = "Paywall — Loading", showBackground = true, heightDp = 800)
@Composable
@Suppress("UnusedPrivateMember")
private fun PreviewPaywallLoading() {
    PairShotTheme {
        PaywallScreen(
            state = PaywallUiState(loading = true),
            dismissible = false,
            onDismiss = {},
            onStartTrial = {},
            onPurchaseYearly = {},
            onPurchaseMonthly = {},
            onContinueFree = {},
            onRestore = {},
            onRetryLoad = {},
            termsUrl = "",
            privacyUrl = "",
        )
    }
}

@Preview(name = "Paywall — Load Error", showBackground = true, heightDp = 800)
@Composable
@Suppress("UnusedPrivateMember")
private fun PreviewPaywallLoadError() {
    PairShotTheme {
        PaywallScreen(
            state = PaywallUiState(loading = false, loadError = true),
            dismissible = false,
            onDismiss = {},
            onStartTrial = {},
            onPurchaseYearly = {},
            onPurchaseMonthly = {},
            onContinueFree = {},
            onRestore = {},
            onRetryLoad = {},
            termsUrl = "",
            privacyUrl = "",
        )
    }
}
