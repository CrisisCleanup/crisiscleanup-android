package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.network.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

private const val CrisisCleanupApiBaseUrl = BuildConfig.API_BASE_URL

private val networkApiJson by lazy {
    Json { ignoreUnknownKeys = true }
}

val crisisCleanupApiBuilder: Retrofit by lazy {
    Retrofit.Builder()
        .baseUrl(CrisisCleanupApiBaseUrl)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(
                    // TODO: Decide logging logic
                    HttpLoggingInterceptor().apply {
                        setLevel(HttpLoggingInterceptor.Level.BODY)
                    }
                )
                .build()
        )
        .addConverterFactory(
            @OptIn(ExperimentalSerializationApi::class)
            networkApiJson.asConverterFactory("application/json".toMediaType())
        )
        .build()
}