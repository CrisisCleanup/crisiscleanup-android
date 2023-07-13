package com.crisiscleanup.network

import com.crisiscleanup.core.common.event.AuthEventBus
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.network.AuthInterceptorProvider
import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import com.crisiscleanup.core.network.RetrofitInterceptorProvider
import com.crisiscleanup.core.network.model.CrisisCleanupNetworkException
import com.crisiscleanup.core.network.model.ExpiredTokenException
import com.crisiscleanup.core.network.model.NetworkCrisisCleanupApiError
import com.crisiscleanup.core.network.model.NetworkErrors
import com.crisiscleanup.core.network.model.ServerErrorException
import com.crisiscleanup.core.network.model.hasExpiredToken
import com.crisiscleanup.core.network.retrofit.RequestHeaderKey
import com.crisiscleanup.core.network.retrofit.RequestHeaderKeysLookup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Singleton

private fun Json.parseNetworkErrors(response: String): List<NetworkCrisisCleanupApiError> {
    var errors: List<NetworkCrisisCleanupApiError> = emptyList()
    try {
        val networkErrors = decodeFromString<NetworkErrors>(response)
        errors = networkErrors.errors
    } catch (e: Exception) {
        // No errors
    }
    return errors
}

private fun Json.parseNetworkErrors(responseBody: ResponseBody): List<NetworkCrisisCleanupApiError> {
    val errorBody = responseBody.string()
    return parseNetworkErrors(errorBody)
}

@Singleton
class CrisisCleanupInterceptorProvider @Inject constructor(
    private val accountDataRepository: AccountDataRepository,
    private val headerKeysLookup: RequestHeaderKeysLookup,
    private val authEventBus: AuthEventBus,
    private val apiClient: CrisisCleanupAuthApi,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : RetrofitInterceptorProvider {
    private val refreshMutex = Mutex()

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private fun getHeaderKey(
        request: Request,
        key: RequestHeaderKey,
    ) = headerKeysLookup.getHeaderKeys(request)?.get(key)

    private val headerInterceptor by lazy {
        Interceptor { chain ->
            var request = chain.request()

            getHeaderKey(request, RequestHeaderKey.AccessTokenAuth)?.let {
                request = setAuthorizationHeader(request)

                return@Interceptor tryAuthRequest(chain, request)
            }

            chain.proceed(request)
        }
    }

    private fun setAuthorizationHeader(request: Request): Request {
        runBlocking {
            val accountData = accountDataRepository.accountData.first()
            if (!accountData.areTokensValid) {
                throw ExpiredTokenException()
            }

            if (accountData.isAccessTokenExpired) {
                if (refreshMutex.tryLock()) {
                    try {
                        if (!refreshTokens()) {
                            throw ExpiredTokenException()
                        }
                    } finally {
                        refreshMutex.unlock()
                    }
                } else {
                    throw ExpiredTokenException()
                }
            }
        }

        val accessToken = accountDataRepository.accessToken
        return request.newBuilder()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
    }

    private fun isExpiredToken(response: Response): Pair<Boolean, Response> {
        if (response.code == 401) {
            return Pair(true, response)
        }
        response.body?.let { responseBody ->
            val body = responseBody.string()
            val errors = json.parseNetworkErrors(body)
            val bodyCopy = body.toResponseBody(responseBody.contentType())
            val copyResponse = response.newBuilder().body(bodyCopy).build()
            return Pair(errors.hasExpiredToken, copyResponse)
        }
        return Pair(false, response)
    }

    private val invalidRefreshTokenErrorMessages = setOf(
        "refresh_token_already_revoked",
        "invalid_refresh_token",
    )

    private suspend fun refreshTokens(): Boolean {
        val refreshToken = accountDataRepository.refreshToken
        if (refreshToken.isNotBlank()) {
            val refreshResult = apiClient.refreshTokens(refreshToken)
            if (refreshResult.error == null) {
                accountDataRepository.updateAccountTokens(
                    refreshResult.refreshToken!!,
                    refreshResult.accessToken!!,
                    Clock.System.now().epochSeconds + refreshResult.expiresIn!!,
                )
                authEventBus.onTokensRefreshed()
                return true
            } else {
                if (invalidRefreshTokenErrorMessages.contains(refreshResult.error)) {
                    accountDataRepository.clearAccountTokens()
                }
            }
        }
        return false
    }

    private fun tryAuthRequest(chain: Interceptor.Chain, request: Request): Response {
        val response = chain.proceed(request)

        val (isExpired, nextResponse) = isExpiredToken(response)
        if (isExpired) {
            runBlocking {
                if (refreshTokens()) {
                    return@runBlocking setAuthorizationHeader(request)
                }
                return@runBlocking null
            }?.let {
                return chain.proceed(it)
            }
            throw ExpiredTokenException()
        }

        return nextResponse
    }

    private val wrapResponseInterceptor by lazy {
        Interceptor { chain ->
            val request = chain.request()
            var response: Response = chain.proceed(request)

            getHeaderKey(request, RequestHeaderKey.WrapResponse)?.let { key ->
                if (response.code == 200) {
                    // TODO Write tests. Including expired tokens where tokens are used.
                    // Would be more elegant to deserialize, make new, and re-serialize.
                    // Data structure is simple so text operations are sufficient.
                    val bodyData = response.body?.string()
                        ?: throw Exception("Unexpected $key response")
                    val wrappedData = """{"$key":$bodyData}"""
                    val converted = wrappedData.toResponseBody(response.body?.contentType())
                    response = response.newBuilder().body(converted).build()
                }
            }

            response
        }
    }

    private val clientErrorInterceptor by lazy {
        Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            if (response.code in 400..499) {
                getHeaderKey(request, RequestHeaderKey.ThrowClientError)?.let {
                    response.body?.let { responseBody ->
                        val errors = json.parseNetworkErrors(responseBody)
                        throw CrisisCleanupNetworkException(
                            request.url.toUrl().toString(),
                            response.code,
                            response.message,
                            errors,
                        )
                    }
                }
            }
            response
        }
    }

    override val serverErrorInterceptor by lazy {
        Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            if (response.code in 500..599) {
                throw ServerErrorException(response)
            }
            response
        }
    }

    override val interceptors: List<Interceptor> = listOf(
        headerInterceptor,
        wrapResponseInterceptor,
        clientErrorInterceptor,
        serverErrorInterceptor,
    )
}

@Singleton
class CrisisCleanupAuthInterceptorProvider @Inject constructor() : AuthInterceptorProvider {
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override val clientErrorInterceptor by lazy {
        Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            if (response.code in 400..499) {
                response.body?.let { responseBody ->
                    val errors = json.parseNetworkErrors(responseBody)
                    throw CrisisCleanupNetworkException(
                        request.url.toUrl().toString(),
                        response.code,
                        response.message,
                        errors,
                    )
                }
            }
            response
        }
    }
}