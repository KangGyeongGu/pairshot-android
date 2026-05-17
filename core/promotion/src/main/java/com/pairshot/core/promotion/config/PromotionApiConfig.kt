package com.pairshot.core.promotion.config

interface PromotionApiConfig {
    val baseUrl: String
    val membershipPath: String
    val activatePath: String
    val byIdPath: String
    val authHeaderName: String?
    val authHeaderValue: String?
    val timeoutMillis: Long
}
