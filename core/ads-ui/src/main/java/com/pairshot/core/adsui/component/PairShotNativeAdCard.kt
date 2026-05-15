package com.pairshot.core.adsui.component

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.pairshot.core.adsui.R
import com.pairshot.core.designsystem.PairShotSpacing

@Composable
fun PairShotNativeAdCard(
    nativeAd: NativeAd,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(NATIVE_AD_ASPECT_RATIO),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        val headlineColor = MaterialTheme.colorScheme.onSurface.toArgb()
        val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
        val ctaBackground = MaterialTheme.colorScheme.primary.toArgb()
        val ctaTextColor = MaterialTheme.colorScheme.onPrimary.toArgb()

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    buildNativeAdView(
                        context = context,
                        headlineColor = headlineColor,
                        bodyColor = bodyColor,
                        ctaBackground = ctaBackground,
                        ctaTextColor = ctaTextColor,
                    )
                },
                update = { view ->
                    bindNativeAd(view, nativeAd)
                },
                onRelease = { view -> view.destroy() },
            )

            Text(
                text = stringResource(R.string.ads_native_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(PairShotSpacing.xs),
            )
        }
    }
}

private fun Context.dpToPx(value: Int): Int = (value * resources.displayMetrics.density).toInt()

private fun buildNativeAdView(
    context: Context,
    headlineColor: Int,
    bodyColor: Int,
    ctaBackground: Int,
    ctaTextColor: Int,
): NativeAdView {
    val adView =
        NativeAdView(context).apply {
            layoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
        }
    val mediaView = createMediaView(context)
    val iconView = createIconView(context)
    val headlineView = createHeadlineView(context, headlineColor)
    val bodyView = createBodyView(context, bodyColor)
    val ctaView = createCtaView(context, ctaBackground, ctaTextColor)
    val textColumn =
        createTextColumn(context).apply {
            addView(iconView)
            addView(headlineView)
            addView(bodyView)
            addView(createSpacer(context))
            addView(ctaView)
        }
    val container =
        createRootRow(context).apply {
            addView(mediaView)
            addView(textColumn)
        }
    adView.addView(container)
    adView.mediaView = mediaView
    adView.headlineView = headlineView
    adView.bodyView = bodyView
    adView.iconView = iconView
    adView.callToActionView = ctaView
    return adView
}

private fun createRootRow(context: Context): LinearLayout =
    LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
    }

private fun createTextColumn(context: Context): LinearLayout =
    LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams =
            LinearLayout
                .LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, NATIVE_TEXT_COLUMN_WEIGHT)
                .apply {
                    leftMargin = context.dpToPx(NATIVE_TEXT_LEFT_MARGIN_DP)
                    topMargin = context.dpToPx(NATIVE_TEXT_VERTICAL_PADDING_DP)
                    bottomMargin = context.dpToPx(NATIVE_TEXT_VERTICAL_PADDING_DP)
                    rightMargin = context.dpToPx(NATIVE_TEXT_RIGHT_MARGIN_DP)
                }
    }

private fun createMediaView(context: Context): MediaView =
    MediaView(context).apply {
        id = View.generateViewId()
        layoutParams =
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, NATIVE_MEDIA_WEIGHT)
    }

private fun createIconView(context: Context): ImageView =
    ImageView(context).apply {
        id = View.generateViewId()
        layoutParams =
            LinearLayout.LayoutParams(
                context.dpToPx(NATIVE_ICON_SIZE_DP),
                context.dpToPx(NATIVE_ICON_SIZE_DP),
            )
    }

private fun createHeadlineView(
    context: Context,
    headlineColor: Int,
): TextView =
    TextView(context).apply {
        id = View.generateViewId()
        setTextColor(headlineColor)
        textSize = NATIVE_HEADLINE_TEXT_SP
        maxLines = NATIVE_HEADLINE_MAX_LINES
        layoutParams =
            LinearLayout
                .LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = context.dpToPx(NATIVE_HEADLINE_TOP_MARGIN_DP) }
    }

private fun createBodyView(
    context: Context,
    bodyColor: Int,
): TextView =
    TextView(context).apply {
        id = View.generateViewId()
        setTextColor(bodyColor)
        textSize = NATIVE_BODY_TEXT_SP
        maxLines = NATIVE_BODY_MAX_LINES
        ellipsize = android.text.TextUtils.TruncateAt.END
        layoutParams =
            LinearLayout
                .LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = context.dpToPx(NATIVE_BODY_TOP_MARGIN_DP) }
    }

private fun createCtaView(
    context: Context,
    ctaBackground: Int,
    ctaTextColor: Int,
): Button =
    Button(context).apply {
        id = View.generateViewId()
        setBackgroundColor(ctaBackground)
        setTextColor(ctaTextColor)
        textSize = NATIVE_CTA_TEXT_SP
        isAllCaps = false
        gravity = Gravity.CENTER
        minHeight = context.dpToPx(NATIVE_CTA_HEIGHT_DP)
        minimumHeight = context.dpToPx(NATIVE_CTA_HEIGHT_DP)
        val horizontalPadding = context.dpToPx(NATIVE_CTA_HORIZONTAL_PADDING_DP)
        setPadding(horizontalPadding, 0, horizontalPadding, 0)
        layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
    }

private fun createSpacer(context: Context): Space =
    Space(context).apply {
        layoutParams = LinearLayout.LayoutParams(0, 0, NATIVE_SPACER_WEIGHT)
    }

private fun bindNativeAd(
    adView: NativeAdView,
    nativeAd: NativeAd,
) {
    (adView.headlineView as? TextView)?.text = nativeAd.headline

    val bodyTextView = adView.bodyView as? TextView
    val bodyText = nativeAd.body
    if (bodyText.isNullOrEmpty()) {
        bodyTextView?.visibility = View.GONE
    } else {
        bodyTextView?.visibility = View.VISIBLE
        bodyTextView?.text = bodyText
    }

    val iconImageView = adView.iconView as? ImageView
    val iconAsset = nativeAd.icon
    if (iconAsset?.drawable != null) {
        iconImageView?.visibility = View.VISIBLE
        iconImageView?.setImageDrawable(iconAsset.drawable)
    } else {
        iconImageView?.visibility = View.GONE
    }

    val ctaButton = adView.callToActionView as? Button
    val ctaText = nativeAd.callToAction
    if (ctaText.isNullOrEmpty()) {
        ctaButton?.visibility = View.GONE
    } else {
        ctaButton?.visibility = View.VISIBLE
        ctaButton?.text = ctaText
    }

    adView.setNativeAd(nativeAd)
}

private const val NATIVE_AD_ASPECT_RATIO = 2.4f
private const val NATIVE_MEDIA_WEIGHT = 1.4f
private const val NATIVE_TEXT_COLUMN_WEIGHT = 2f
private const val NATIVE_SPACER_WEIGHT = 1f
private const val NATIVE_TEXT_LEFT_MARGIN_DP = 10
private const val NATIVE_TEXT_RIGHT_MARGIN_DP = 8
private const val NATIVE_TEXT_VERTICAL_PADDING_DP = 6
private const val NATIVE_ICON_SIZE_DP = 20
private const val NATIVE_HEADLINE_TOP_MARGIN_DP = 2
private const val NATIVE_BODY_TOP_MARGIN_DP = 2
private const val NATIVE_HEADLINE_TEXT_SP = 13f
private const val NATIVE_BODY_TEXT_SP = 11f
private const val NATIVE_CTA_TEXT_SP = 12f
private const val NATIVE_CTA_HEIGHT_DP = 28
private const val NATIVE_CTA_HORIZONTAL_PADDING_DP = 12
private const val NATIVE_HEADLINE_MAX_LINES = 2
private const val NATIVE_BODY_MAX_LINES = 1
