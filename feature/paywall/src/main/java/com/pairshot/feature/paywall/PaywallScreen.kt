package com.pairshot.feature.paywall

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.pairshot.core.billing.domain.BillingOffer
import com.pairshot.core.designsystem.PairShotRadius
import com.pairshot.core.designsystem.PairShotScreen
import com.pairshot.core.designsystem.PairShotSnackbarTokens
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotTouchTarget
import com.pairshot.core.ui.component.PairShotSnackbarController
import com.pairshot.core.ui.component.PairShotSnackbarHost
import com.pairshot.feature.paywall.component.PaywallPlanSection

private val LOGO_SIZE = PairShotTouchTarget.large
private val LOGO_CORNER = PairShotSpacing.md
private val LOGO_SHADOW_ELEVATION = PairShotSpacing.md
private const val LOGO_SHADOW_AMBIENT_ALPHA = 0.18f
private const val LOGO_SHADOW_SPOT_ALPHA = 0.28f
private const val CONTINUE_FREE_ALPHA = 0.9f
private const val LEGAL_ALPHA = 0.65f
private const val DISCLOSURE_ALPHA = 0.6f

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
    modifier: Modifier = Modifier,
    snackbarController: PairShotSnackbarController = remember { PairShotSnackbarController() },
) {
    val uriHandler = LocalUriHandler.current
    if (!dismissible) BackHandler { }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            TopBar(dismissible = dismissible, onDismiss = onDismiss)

            Column(
                modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PairShotSpacing.xl),
            ) {
                Spacer(modifier = Modifier.height(PairShotSpacing.xxl))
                Hero()

                Spacer(modifier = Modifier.height(PairShotSpacing.xxxl))
                ValueProp(stringResource(R.string.paywall_value_unlimited))
                Spacer(modifier = Modifier.height(PairShotSpacing.lg))
                ValueProp(stringResource(R.string.paywall_value_no_ads))
                Spacer(modifier = Modifier.height(PairShotSpacing.lg))
                ValueProp(stringResource(R.string.paywall_value_pro_features))

                Spacer(modifier = Modifier.height(PairShotSpacing.xxxl))
                PaywallPlanSection(
                    state = state,
                    onStartTrial = onStartTrial,
                    onPurchaseYearly = onPurchaseYearly,
                    onPurchaseMonthly = onPurchaseMonthly,
                    onRetryLoad = onRetryLoad,
                )

                Spacer(modifier = Modifier.height(PairShotScreen.horizontalPadding))
                CenteredTextLink(
                    text = stringResource(R.string.paywall_restore),
                    onClick = onRestore,
                )
                if (!dismissible) {
                    CenteredTextLink(
                        text = stringResource(R.string.paywall_continue_free),
                        onClick = onContinueFree,
                        alpha = CONTINUE_FREE_ALPHA,
                    )
                }

                Spacer(modifier = Modifier.height(PairShotScreen.horizontalPadding))
                DisclosureBlock()
                Spacer(modifier = Modifier.height(PairShotSpacing.lg))
            }

            LegalRow(
                modifier = Modifier.padding(horizontal = PairShotSpacing.xl),
                onOpenTerms = { uriHandler.openUri(termsUrl) },
                onOpenPrivacy = { uriHandler.openUri(privacyUrl) },
            )
            Spacer(modifier = Modifier.height(PairShotSpacing.md))
        }

        PairShotSnackbarHost(
            controller = snackbarController,
            modifier =
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = PairShotSnackbarTokens.topOffset),
        )
    }
}

@Composable
private fun DisclosureBlock() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DISCLOSURE_ALPHA)
    val style = MaterialTheme.typography.labelSmall
    Column(verticalArrangement = Arrangement.spacedBy(PairShotRadius.sm)) {
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
                .padding(start = PairShotSpacing.xs, top = PairShotSpacing.xs),
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
        Spacer(modifier = Modifier.height(PairShotSpacing.xl))
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
        Spacer(modifier = Modifier.height(PairShotSpacing.md))
        Text(
            text = stringResource(R.string.paywall_title),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(PairShotSpacing.xs))
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
            modifier = Modifier.padding(end = PairShotSpacing.md),
        )
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
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
            .padding(vertical = PairShotSpacing.md),
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
            modifier = Modifier.clickable(onClick = onOpenTerms).padding(PairShotSpacing.sm),
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
            modifier = Modifier.clickable(onClick = onOpenPrivacy).padding(PairShotSpacing.sm),
        )
    }
}
