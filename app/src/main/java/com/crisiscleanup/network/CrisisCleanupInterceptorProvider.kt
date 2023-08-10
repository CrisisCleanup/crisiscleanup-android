package com.crisiscleanup.network

import android.util.Log
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
import com.crisiscleanup.core.network.model.SingleNetworkError
import com.crisiscleanup.core.network.model.hasExpiredToken
import com.crisiscleanup.core.network.retrofit.RequestHeaderKey
import com.crisiscleanup.core.network.retrofit.RequestHeaderKeysLookup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private fun Json.parseNetworkErrors(response: String): List<NetworkCrisisCleanupApiError> {
    var errors: List<NetworkCrisisCleanupApiError> = emptyList()
    try {
        val networkErrors = decodeFromString<NetworkErrors>(response)
        errors = networkErrors.errors
    } catch (e: Exception) {
        // No errors or not formatted as expected
    }
    return errors
}

private fun Json.parseNetworkErrors(responseBody: ResponseBody): List<NetworkCrisisCleanupApiError> {
    val errorBody = responseBody.string()
    return parseNetworkErrors(errorBody)
}

private val Request.pathsForLog: String
    get() = url.pathSegments.joinToString("/")

@Singleton
class CrisisCleanupInterceptorProvider @Inject constructor(
    private val accountDataRepository: AccountDataRepository,
    private val headerKeysLookup: RequestHeaderKeysLookup,
    private val authEventBus: AuthEventBus,
    private val apiClient: CrisisCleanupAuthApi,
    @Logger(CrisisCleanupLoggers.Network) private val logger: AppLogger,
) : RetrofitInterceptorProvider {

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

            if (accountData.isAccessTokenExpired &&
                !refreshTokens()
            ) {
                throw ExpiredTokenException()
            }
        }

        val accessToken = accountDataRepository.accessToken
        return request.newBuilder()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
    }

    private fun isExpiredToken(response: Response, logPaths: String): Pair<Boolean, Response> {
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
        // TODO If body is null from above wouldn't response need to close (and rebuild)?
        logger.logCapture("Token was not expired and body was null for $logPaths. Incoming exception?")
        return Pair(false, response)
    }

    private val invalidRefreshTokenErrorMessages = setOf(
        "refresh_token_already_revoked",
        "invalid_refresh_token",
    )

    private suspend fun refreshTokens(): Boolean {
        val refreshToken = accountDataRepository.refreshToken
        if (refreshToken.isNotBlank()) {
            try {
                apiClient.refreshTokens(refreshToken)?.let { refreshResult ->
                    accountDataRepository.updateAccountTokens(
                        refreshResult.refreshToken,
                        refreshResult.accessToken,
                        Clock.System.now().epochSeconds + refreshResult.expiresIn,
                    )
                    authEventBus.onTokensRefreshed()
                    return true
                }
            } catch (e: NetworkAuthException) {
                if (invalidRefreshTokenErrorMessages.contains(e.message)) {
                    accountDataRepository.clearAccountTokens()
                }
            }
        }
        return false
    }

    private fun tryAuthRequest(chain: Interceptor.Chain, request: Request): Response {
        val response = chain.proceed(request)

        val (isExpired, nextResponse) = isExpiredToken(
            response,
            request.pathsForLog,
        )
        if (isExpired) {
            logger.logCapture("Expired token trying refresh ${request.pathsForLog}")
            runBlocking {
                if (refreshTokens()) {
                    logger.logCapture(("Closing response before  ${request.pathsForLog}"))
                    response.close()
                    return@runBlocking setAuthorizationHeader(request)
                }
                return@runBlocking null
            }?.let {
                return chain.proceed(it)
            }
            throw ExpiredTokenException()
        }

        logger.logCapture("Valid token copy response ${request.pathsForLog}")
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
                        logger.logCapture("Code ${response.code} message ${response.message} paths ${request.pathsForLog}")
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
                logger.logCapture("Code ${response.code} message ${response.message} paths ${request.pathsForLog}")
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
                    val errorBody = responseBody.string()
                    var authErrorMessage = ""
                    try {
                        val singleError = json.decodeFromString<SingleNetworkError>(errorBody)
                        authErrorMessage = singleError.error
                    } catch (e: Exception) {
                        Log.w(
                            "network-error",
                            "Network auth error format not parsed $errorBody",
                        )
                    }
                    if (authErrorMessage.isNotBlank()) {
                        throw NetworkAuthException(authErrorMessage)
                    } else {
                        throw CrisisCleanupNetworkException(
                            request.url.toUrl().toString(),
                            response.code,
                            response.message,
                            emptyList(),
                        )
                    }
                }
            }
            response
        }
    }
}

private class NetworkAuthException(message: String) : IOException(message)