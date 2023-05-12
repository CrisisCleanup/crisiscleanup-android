package com.crisiscleanup.network

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.network.RetrofitInterceptorProvider
import com.crisiscleanup.core.network.model.CrisisCleanupNetworkException
import com.crisiscleanup.core.network.model.NetworkCrisisCleanupApiError
import com.crisiscleanup.core.network.model.NetworkErrors
import com.crisiscleanup.core.network.retrofit.RequestHeaderKey
import com.crisiscleanup.core.network.retrofit.RequestHeaderKeysLookup
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupInterceptorProvider @Inject constructor(
    private val accountDataRepository: AccountDataRepository,
    private val headerKeysLookup: RequestHeaderKeysLookup,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : RetrofitInterceptorProvider {
    private val json = Json { ignoreUnknownKeys = true }

    private fun getHeaderKey(
        request: Request,
        key: RequestHeaderKey,
    ) = headerKeysLookup.getHeaderKeys(request)?.get(key)

    private val headerInterceptor: Interceptor by lazy {
        Interceptor { chain ->
            var request = chain.request()

            getHeaderKey(request, RequestHeaderKey.AccessTokenAuth)?.let {
                val accessToken = accountDataRepository.accessTokenCached
                if (accessToken.isNotEmpty()) {
                    request = request.newBuilder()
                        .addHeader("Authorization", "Bearer $accessToken")
                        .build()
                }
            }

            chain.proceed(request)
        }
    }

    private val wrapResponseInterceptor: Interceptor by lazy {
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

    private val clientErrorInterceptor: Interceptor by lazy {
        Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            if (response.code in 400..499) {
                getHeaderKey(request, RequestHeaderKey.ThrowClientError)?.let {
                    response.body?.let { responseBody ->
                        val errorBody = responseBody.string()
                        var errors: List<NetworkCrisisCleanupApiError> = emptyList()
                        try {
                            val networkErrors = json.decodeFromString<NetworkErrors>(errorBody)
                            errors = networkErrors.errors
                        } catch (e: Exception) {
                            // Ignore serialize fail
                        }
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

    override val interceptors: List<Interceptor> = listOf(
        headerInterceptor,
        wrapResponseInterceptor,
        clientErrorInterceptor,
    )
}