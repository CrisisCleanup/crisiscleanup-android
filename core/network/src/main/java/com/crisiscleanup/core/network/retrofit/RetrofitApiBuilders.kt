package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.AppSettingsProvider
import com.crisiscleanup.core.network.RetrofitInterceptorProvider
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class RetrofitConfiguration(val retrofit: RetrofitConfigurations)

internal enum class RetrofitConfigurations {
    Basic,
    BasicJson,
    Auth,
    CrisisCleanup,
}

private fun getClientBuilder(isDebuggable: Boolean = false): OkHttpClient.Builder {
    val clientBuilder = OkHttpClient.Builder()
        // TODO Allow user configuration? Or adjust dynamically according to device and network conditions?
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)

    if (isDebuggable) {
        clientBuilder.addInterceptor(
            HttpLoggingInterceptor().apply {
                setLevel(HttpLoggingInterceptor.Level.HEADERS)
            },
        )
    }

    return clientBuilder
}

private val Json.converterFactory: Converter.Factory
    get() = asConverterFactory("application/json".toMediaType())

internal fun getCrisisCleanupApiBuilder(
    appEnv: AppEnv,
    settingsProvider: AppSettingsProvider,
    interceptors: Collection<Interceptor>,
    networkApiJson: Json,
): Retrofit {
    val clientBuilder = getClientBuilder(appEnv.isDebuggable)

    interceptors.forEach {
        clientBuilder.addInterceptor(it)
    }

    return Retrofit.Builder()
        .baseUrl(settingsProvider.apiBaseUrl)
        .client(clientBuilder.build())
        .addConverterFactory(networkApiJson.converterFactory)
        .build()
}

internal fun getCrisisCleanupApiBuilder(
    appEnv: AppEnv,
    settingsProvider: AppSettingsProvider,
    interceptorProvider: RetrofitInterceptorProvider,
    headerKeysLookup: RequestHeaderKeysLookup,
    networkApiJson: Json,
): Retrofit {
    val clientBuilder = getClientBuilder(appEnv.isDebuggable)

    interceptorProvider.interceptors?.forEach {
        clientBuilder.addInterceptor(it)
    }

    return Retrofit.Builder()
        .baseUrl(settingsProvider.apiBaseUrl)
        .client(clientBuilder.build())
        .addCallAdapterFactory(RequestHeaderCallAdapterFactory(headerKeysLookup))
        .addConverterFactory(networkApiJson.converterFactory)
        .build()
}

internal fun getApiBuilder(
    appEnv: AppEnv,
    settingsProvider: AppSettingsProvider,
    interceptors: List<Interceptor>,
): Retrofit {
    val clientBuilder = getClientBuilder(appEnv.isDebuggable)

    interceptors.forEach {
        clientBuilder.addInterceptor(it)
    }

    return Retrofit.Builder()
        .baseUrl(settingsProvider.apiBaseUrl)
        .client(clientBuilder.build())
        .build()
}

internal fun getJsonApiBuilder(
    appEnv: AppEnv,
    settingsProvider: AppSettingsProvider,
    interceptors: List<Interceptor>,
    networkApiJson: Json,
): Retrofit {
    val clientBuilder = getClientBuilder(appEnv.isDebuggable)

    interceptors.forEach {
        clientBuilder.addInterceptor(it)
    }

    return Retrofit.Builder()
        .baseUrl(settingsProvider.apiBaseUrl)
        .client(clientBuilder.build())
        .addConverterFactory(networkApiJson.converterFactory)
        .build()
}
