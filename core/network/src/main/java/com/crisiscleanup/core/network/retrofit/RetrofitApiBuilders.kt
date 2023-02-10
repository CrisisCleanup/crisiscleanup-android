package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.network.BuildConfig
import com.crisiscleanup.core.network.RetrofitInterceptorProvider
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Qualifier

private const val CrisisCleanupApiBaseUrl = BuildConfig.API_BASE_URL

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class CrisisCleanupRetrofit

internal fun getCrisisCleanupApiBuilder(
    interceptorProvider: RetrofitInterceptorProvider,
    headerKeysLookup: RequestHeaderKeysLookup,
    networkApiJson: Json,
    appEnv: AppEnv,
): Retrofit {
    val clientBuilder = OkHttpClient.Builder()

    if (appEnv.isDebuggable) {
        clientBuilder.addInterceptor(HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.HEADERS)
        })
    }

    interceptorProvider.interceptors?.forEach {
        clientBuilder.addInterceptor(it)
    }

    return Retrofit.Builder()
        .baseUrl(CrisisCleanupApiBaseUrl)
        .client(clientBuilder.build())
        .addCallAdapterFactory(RequestHeaderCallAdapterFactory(headerKeysLookup))
        .addConverterFactory(
            @OptIn(ExperimentalSerializationApi::class)
            networkApiJson.asConverterFactory("application/json".toMediaType())
        )
        .build()
}