package com.crisiscleanup.network

import com.crisiscleanup.core.network.AccessTokenManager
import com.crisiscleanup.core.network.RetrofitInterceptorProvider
import okhttp3.Interceptor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupHeaderInterceptorProvider @Inject constructor(
    private val accessTokenManager: AccessTokenManager
) : RetrofitInterceptorProvider {
    override val interceptors: List<Interceptor>?
        get() {
            // TODO Research if there is a pattern for adding headers to specific routes
            val headerInterceptor = Interceptor {
                val accessToken = accessTokenManager.accessToken
                if (accessToken.isEmpty()) {
                    return@Interceptor it.proceed(it.request())
                }

                val request = it.request().newBuilder()
                    .addHeader("Authorization", "Bearer $accessToken")
                    .build()
                return@Interceptor it.proceed(request)
            }
            return listOf(headerInterceptor)
        }
}