package com.crisiscleanup.core.network

import okhttp3.Interceptor

interface RetrofitInterceptorProvider {
    val interceptors: List<Interceptor>?
}
