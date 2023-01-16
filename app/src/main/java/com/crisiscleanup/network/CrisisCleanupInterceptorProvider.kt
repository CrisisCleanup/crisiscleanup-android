package com.crisiscleanup.network

import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.network.RetrofitInterceptorProvider
import com.crisiscleanup.core.network.retrofit.RequestHeaderKey
import com.crisiscleanup.core.network.retrofit.RequestHeaderKeysLookup
import okhttp3.Interceptor
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
                                requestBuilder.addHeader(
                                    "Authorization",
                                    "Bearer $accessToken"
                                )
                                addHeaderCount++
                            }
                        }
                    }
                }
            }

            return@Interceptor if (addHeaderCount > 0) chain.proceed(requestBuilder.build())
            else chain.proceed(request)
        }
    }

    override val interceptors: List<Interceptor> = listOf(
        headerInterceptor,
    )
}