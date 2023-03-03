package com.crisiscleanup.network

import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.network.RetrofitInterceptorProvider
import com.crisiscleanup.core.network.retrofit.RequestHeaderKey
import com.crisiscleanup.core.network.retrofit.RequestHeaderKeysLookup
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupInterceptorProvider @Inject constructor(
    private val accountDataRepository: AccountDataRepository,
    private val headerKeysLookup: RequestHeaderKeysLookup,
) : RetrofitInterceptorProvider {
    private val headerInterceptor: Interceptor by lazy {
        Interceptor { chain ->
            val request = chain.request()

            val requestBuilder = request.newBuilder()
            var addHeaderCount = 0
            headerKeysLookup.getHeaderKeys(request)?.let {
                it.forEach { entry ->
                    when (entry.key) {
                        RequestHeaderKey.AccessTokenAuth -> {
                            val accessToken = accountDataRepository.accessTokenCached
                            if (accessToken.isNotEmpty()) {
                                requestBuilder.addHeader("Authorization", "Bearer $accessToken")
                                addHeaderCount++
                            }
                        }
                        else -> {}
                    }
                }
            }

            return@Interceptor if (addHeaderCount > 0) chain.proceed(requestBuilder.build())
            else chain.proceed(request)
        }
    }

    private val wrapPayloadInterceptor: Interceptor by lazy {
        Interceptor { chain ->
            val request = chain.request()
            var response: Response = chain.proceed(request)

            headerKeysLookup.getHeaderKeys(request)?.let {

                it[RequestHeaderKey.WrapResponse]?.let { key ->
                    if (response.code == 200) {
                        // TODO Write tests
                        // Better to deserialize, make new, and re-serialize but data structure is simple so text operations is sufficient
                        val bodyData = response.body?.string()
                            ?: throw Exception("Unexpected $key response")
                        val wrappedData = """{"$key":$bodyData}"""
                        val converted = wrappedData.toResponseBody(response.body?.contentType())
                        response = response.newBuilder().body(converted).build()
                    }
                }
            }

            response
        }
    }

    override val interceptors: List<Interceptor> = listOf(
        headerInterceptor,
        wrapPayloadInterceptor,
    )
}