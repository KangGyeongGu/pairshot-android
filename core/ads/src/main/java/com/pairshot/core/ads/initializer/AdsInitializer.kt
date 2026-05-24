package com.pairshot.core.ads.initializer

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdsInitializer
@Inject
constructor() {
    private val initialized = AtomicBoolean(false)
    private val ready = CompletableDeferred<Unit>()

    @Volatile
    private var consentInformation: ConsentInformation? = null

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    fun initialize(activity: Activity) {
        if (!initialized.compareAndSet(false, true)) return
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation = info
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Timber.tag(TAG).w("Consent form error: code=%d %s", formError.errorCode, formError.message)
                    }
                    refreshPrivacyOptionsRequired()
                    initializeMobileAdsIfAllowed(activity)
                }
            },
            { requestError ->
                Timber.tag(
                    TAG
                ).w("Consent info update failed: code=%d %s", requestError.errorCode, requestError.message)
                refreshPrivacyOptionsRequired()
                initializeMobileAdsIfAllowed(activity)
            },
        )
    }

    private fun initializeMobileAdsIfAllowed(context: Context) {
        val info = consentInformation
        if (info != null && !info.canRequestAds()) {
            Timber.tag(TAG).d("Consent not granted — skipping MobileAds.initialize")
            ready.complete(Unit)
            return
        }
        runCatching {
            MobileAds.initialize(context.applicationContext) { status ->
                Timber.tag(TAG).d("MobileAds initialized: %s", status.adapterStatusMap)
                ready.complete(Unit)
            }
        }.onFailure { error ->
            Timber.tag(TAG).e(error, "MobileAds initialize failed")
            ready.complete(Unit)
        }
    }

    fun showPrivacyOptionsForm(
        activity: Activity,
        onComplete: (FormError?) -> Unit,
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) {
                Timber.tag(TAG).w("Privacy options form error: code=%d %s", error.errorCode, error.message)
            }
            refreshPrivacyOptionsRequired()
            onComplete(error)
        }
    }

    private fun refreshPrivacyOptionsRequired() {
        val info = consentInformation ?: return
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    suspend fun awaitReady() {
        withTimeoutOrNull(INIT_TIMEOUT_MS) { ready.await() }
    }

    private companion object {
        const val TAG = "AdsInitializer"
        const val INIT_TIMEOUT_MS = 5_000L
    }
}
