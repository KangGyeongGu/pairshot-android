package com.pairshot.core.promotion.remote

import com.pairshot.core.promotion.config.PromotionApiConfig
import com.pairshot.core.promotion.remote.dto.ActivateRequestDto
import com.pairshot.core.promotion.remote.dto.ActivateResponseDto
import com.pairshot.core.promotion.remote.dto.ErrorResponseDto
import com.pairshot.core.promotion.remote.dto.MembershipDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KtorPromotionApi
    @Inject
    constructor(
        private val apiConfig: PromotionApiConfig,
    ) : PromotionApi {
        private val client: HttpClient by lazy {
            HttpClient(OkHttp) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                            encodeDefaults = false
                        },
                    )
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = apiConfig.timeoutMillis
                    connectTimeoutMillis = apiConfig.timeoutMillis
                    socketTimeoutMillis = apiConfig.timeoutMillis
                }
            }
        }

        override suspend fun fetchMembership(deviceHash: String): MembershipApiResult {
            if (apiConfig.baseUrl.isBlank()) {
                Timber.tag(API_TAG).w("Promotion API base URL blank — fetch disabled")
                return MembershipApiResult.NetworkError
            }
            val url = apiConfig.baseUrl.trimEnd('/') + apiConfig.membershipPath
            val response = safeGet(url) { parameter("device", deviceHash) } ?: return MembershipApiResult.NetworkError
            return mapMembershipResponse(response)
        }

        private suspend fun safeGet(
            url: String,
            block: io.ktor.client.request.HttpRequestBuilder.() -> Unit,
        ): HttpResponse? =
            try {
                client.get(url) {
                    header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    block()
                }
            } catch (t: java.io.IOException) {
                Timber.tag(API_TAG).w(t, "GET network I/O error url=%s", url)
                null
            } catch (t: io.ktor.client.plugins.HttpRequestTimeoutException) {
                Timber.tag(API_TAG).w(t, "GET timeout url=%s", url)
                null
            }

        private suspend fun mapMembershipResponse(response: HttpResponse): MembershipApiResult =
            when (response.status) {
                HttpStatusCode.OK -> {
                    runCatching { response.body<MembershipDto>() }
                        .map { MembershipApiResult.Success(it) as MembershipApiResult }
                        .getOrElse {
                            Timber.tag(API_TAG).w(it, "membership 200 body decode failed")
                            MembershipApiResult.ServerError
                        }
                }

                HttpStatusCode.TooManyRequests -> MembershipApiResult.ServerError

                else -> {
                    val bodyText = runCatching { response.bodyAsText() }.getOrDefault("<read failed>")
                    Timber.tag(API_TAG).w(
                        "membership unexpected status=%d body=%s",
                        response.status.value,
                        bodyText,
                    )
                    MembershipApiResult.ServerError
                }
            }

        override suspend fun activate(request: ActivateRequestDto): ActivationApiResult {
            if (apiConfig.baseUrl.isBlank()) {
                Timber.tag(API_TAG).w("Promotion API base URL blank — activation disabled")
                return ActivationApiResult.NetworkError
            }
            val url = apiConfig.baseUrl.trimEnd('/') + apiConfig.activatePath
            val response = safePost(url) { setBody(request) } ?: return ActivationApiResult.NetworkError
            return mapActivationResponse(response)
        }

        private suspend fun safePost(
            url: String,
            block: io.ktor.client.request.HttpRequestBuilder.() -> Unit,
        ): HttpResponse? =
            try {
                client.post(url) {
                    contentType(ContentType.Application.Json)
                    apiConfig.authHeaderName?.let { name ->
                        apiConfig.authHeaderValue?.let { value -> header(name, value) }
                    }
                    header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    block()
                }
            } catch (t: java.io.IOException) {
                Timber.tag(API_TAG).w(t, "POST network I/O error url=%s", url)
                null
            } catch (t: io.ktor.client.plugins.HttpRequestTimeoutException) {
                Timber.tag(API_TAG).w(t, "POST timeout url=%s", url)
                null
            }

        private suspend fun mapActivationResponse(response: HttpResponse): ActivationApiResult =
            when (response.status) {
                HttpStatusCode.OK -> {
                    runCatching { response.body<ActivateResponseDto>() }
                        .map { ActivationApiResult.Success(it) as ActivationApiResult }
                        .getOrElse {
                            Timber.tag(API_TAG).w(it, "activate 200 body decode failed")
                            ActivationApiResult.ServerError
                        }
                }

                HttpStatusCode.NotFound -> ActivationApiResult.NotFound

                HttpStatusCode.Conflict -> ActivationApiResult.AlreadyUsedOnAnotherDevice

                HttpStatusCode.Gone -> ActivationApiResult.Revoked

                HttpStatusCode.Unauthorized -> {
                    Timber.tag(API_TAG).w("activate 401 — check PROMOTION_API_AUTH_KEY")
                    ActivationApiResult.ServerError
                }

                HttpStatusCode.BadRequest -> {
                    val body = runCatching { response.body<ErrorResponseDto>() }.getOrNull()
                    when (body?.error) {
                        "INVALID_CODE_FORMAT" -> ActivationApiResult.InvalidCodeFormat
                        "INVALID_SIGNATURE" -> ActivationApiResult.InvalidSignature
                        "INVALID_DEVICE" -> ActivationApiResult.InvalidCodeFormat
                        else -> {
                            Timber.tag(API_TAG).w("400 unrecognized error=%s", body?.error)
                            ActivationApiResult.InvalidCodeFormat
                        }
                    }
                }

                HttpStatusCode.TooManyRequests -> ActivationApiResult.ServerError

                else -> {
                    val bodyText = runCatching { response.bodyAsText() }.getOrDefault("<read failed>")
                    Timber.tag(API_TAG).w(
                        "activate unexpected status=%d body=%s",
                        response.status.value,
                        bodyText,
                    )
                    ActivationApiResult.ServerError
                }
            }

        private companion object {
            const val API_TAG = "PromotionApi"
        }
    }
