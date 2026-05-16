package com.pairshot.core.promotion.config

import com.pairshot.core.promotion.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildConfigPromotionApiConfig
    @Inject
    constructor() : PromotionApiConfig {
        override val baseUrl: String = BuildConfig.PROMOTION_API_BASE_URL
        override val membershipPath: String = "/promotion"
        override val activatePath: String = "/promotions/activate"
        override val byIdPath: String = "/promotions"
        override val authHeaderName: String? =
            if (BuildConfig.PROMOTION_API_AUTH_KEY.isNotBlank()) "Authorization" else null
        override val authHeaderValue: String? =
            if (BuildConfig.PROMOTION_API_AUTH_KEY.isNotBlank()) "Bearer ${BuildConfig.PROMOTION_API_AUTH_KEY}" else null
        override val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS

        private companion object {
            const val DEFAULT_TIMEOUT_MILLIS = 10_000L
        }
    }
