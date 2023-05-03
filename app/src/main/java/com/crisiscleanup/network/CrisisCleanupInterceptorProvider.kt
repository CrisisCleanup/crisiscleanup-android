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
            var request = chain.request()

            headerKeysLookup.getHeaderKeys(request)?.let {
                val requestBuilder = request.newBuilder()
                var addHeaderCount = 0

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


                if (addHeaderCount > 0) {
                    request = requestBuilder.build()
                }
            }

            chain.proceed(request)
        }
    }

    private val wrapResponseInterceptor: Interceptor by lazy {
        Interceptor { chain ->
            val request = chain.request()
            var response: Response = chain.proceed(request)

            headerKeysLookup.getHeaderKeys(request)?.let {

                it[RequestHeaderKey.WrapResponse]?.let { key ->
                    if (response.code == 200) {
                        // TODO Write tests. Including expired tokens where tokens are used.
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
        wrapResponseInterceptor,
    )
}