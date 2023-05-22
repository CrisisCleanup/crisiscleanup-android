package com.crisiscleanup.core.network

import okhttp3.Interceptor

interface RetrofitInterceptorProvider {
    val serverErrorInterceptor: Interceptor
    val interceptors: List<Interceptor>?
}
