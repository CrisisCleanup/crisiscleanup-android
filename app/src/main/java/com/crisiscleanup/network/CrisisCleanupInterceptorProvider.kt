package com.crisiscleanup.network

import com.crisiscleanup.core.common.event.AuthEventBus
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.network.RetrofitInterceptorProvider
import com.crisiscleanup.core.network.model.CrisisCleanupNetworkException
import com.crisiscleanup.core.network.model.ExpiredTokenException
import com.crisiscleanup.core.network.model.NetworkCrisisCleanupApiError
import com.crisiscleanup.core.network.model.NetworkErrors
import com.crisiscleanup.core.network.model.ServerErrorException
import com.crisiscleanup.core.network.retrofit.RequestHeaderKey
import com.crisiscleanup.core.network.retrofit.RequestHeaderKeysLookup
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupInterceptorProvider @Inject constructor(
    private val accountDataRepository: AccountDataRepository,
    private val headerKeysLookup: RequestHeaderKeysLookup,
    private val authEventBus: AuthEventBus,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
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
                request = addAuthorizationHeader(request)
            }

            chain.proceed(request)
        }
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

    private fun parseNetworkErrors(responseBody: ResponseBody): List<NetworkCrisisCleanupApiError> {
        val errorBody = responseBody.string()
        var errors: List<NetworkCrisisCleanupApiError> = emptyList()
        try {
            val networkErrors = json.decodeFromString<NetworkErrors>(errorBody)
            errors = networkErrors.errors
        } catch (e: Exception) {
            // No errors
        }
        return errors
    }

    private val expiredTokenInterceptor by lazy {
        Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            if (response.code in 400..499) {
                if (response.code == 401) {
                    authEventBus.onExpiredToken()
                    throw ExpiredTokenException()
                }
                response.body?.let { responseBody ->
                    val errors = parseNetworkErrors(responseBody)
                    if (errors.any(NetworkCrisisCleanupApiError::isExpiredToken)) {
                        authEventBus.onExpiredToken()
                        throw ExpiredTokenException()
                    }
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
                        val errors = parseNetworkErrors(responseBody)
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
        expiredTokenInterceptor,
        clientErrorInterceptor,
        serverErrorInterceptor,
    )

    private fun addAuthorizationHeader(request: Request): Request {
        val accessToken = accountDataRepository.accessTokenCached
        return if (accessToken.isNotEmpty()) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
        } else {
            request
        }
    }
}