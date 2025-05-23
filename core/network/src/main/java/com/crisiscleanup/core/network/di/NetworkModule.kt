package com.crisiscleanup.core.network.di

import android.content.Context
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.util.DebugLogger
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.AppSettingsProvider
import com.crisiscleanup.core.network.AuthInterceptorProvider
import com.crisiscleanup.core.network.CrisisCleanupAccountApi
import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.CrisisCleanupRegisterApi
import com.crisiscleanup.core.network.CrisisCleanupWriteApi
import com.crisiscleanup.core.network.RetrofitInterceptorProvider
import com.crisiscleanup.core.network.appsupport.AppSupportApiClient
import com.crisiscleanup.core.network.appsupport.AppSupportClient
import com.crisiscleanup.core.network.fake.FakeAssetManager
import com.crisiscleanup.core.network.retrofit.AccountApiClient
import com.crisiscleanup.core.network.retrofit.AuthApiClient
import com.crisiscleanup.core.network.retrofit.DataApiClient
import com.crisiscleanup.core.network.retrofit.RegisterApiClient
import com.crisiscleanup.core.network.retrofit.RequestHeaderKeysLookup
import com.crisiscleanup.core.network.retrofit.RetrofitConfiguration
import com.crisiscleanup.core.network.retrofit.RetrofitConfigurations
import com.crisiscleanup.core.network.retrofit.WriteApiClient
import com.crisiscleanup.core.network.retrofit.getApiBuilder
import com.crisiscleanup.core.network.retrofit.getCrisisCleanupApiBuilder
import com.crisiscleanup.core.network.retrofit.getJsonApiBuilder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface NetworkInterfaceModule {
    @Binds
    fun bindsAuthApiClient(apiClient: AuthApiClient): CrisisCleanupAuthApi

    @Binds
    fun bindsDataApiClient(apiClient: DataApiClient): CrisisCleanupNetworkDataSource

    @Binds
    fun bindsWriteApiClient(apiClient: WriteApiClient): CrisisCleanupWriteApi

    @Binds
    fun bindsAccountApiClient(apiClient: AccountApiClient): CrisisCleanupAccountApi

    @Binds
    fun bindsRegisterApiClient(apiClient: RegisterApiClient): CrisisCleanupRegisterApi

    @Binds
    fun bindsAppSupportApiClient(apiClient: AppSupportApiClient): AppSupportClient
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    fun providesNetworkJson() = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun providesFakeAssetManager(
        @ApplicationContext context: Context,
    ) = FakeAssetManager(context.assets::open)

    @Provides
    @Singleton
    fun providesRequestHeaderKeysLookup() = RequestHeaderKeysLookup()

    @RetrofitConfiguration(RetrofitConfigurations.Auth)
    @Provides
    @Singleton
    fun providesCrisisCleanupAuthRetrofit(
        appEnv: AppEnv,
        settingsProvider: AppSettingsProvider,
        interceptorProvider: AuthInterceptorProvider,
        json: Json,
    ): Retrofit {
        val interceptors = listOf(interceptorProvider.clientErrorInterceptor)
        return getCrisisCleanupApiBuilder(
            appEnv,
            settingsProvider,
            interceptors,
            json,
        )
    }

    @RetrofitConfiguration(RetrofitConfigurations.CrisisCleanup)
    @Provides
    @Singleton
    fun providesCrisisCleanupRetrofit(
        appEnv: AppEnv,
        settingsProvider: AppSettingsProvider,
        interceptorProvider: RetrofitInterceptorProvider,
        headerKeysLookup: RequestHeaderKeysLookup,
        json: Json,
    ) = getCrisisCleanupApiBuilder(
        appEnv,
        settingsProvider,
        interceptorProvider,
        headerKeysLookup,
        json,
    )

    @RetrofitConfiguration(RetrofitConfigurations.Basic)
    @Provides
    @Singleton
    fun providesBasicRetrofit(
        appEnv: AppEnv,
        settingsProvider: AppSettingsProvider,
        interceptorProvider: RetrofitInterceptorProvider,
    ): Retrofit {
        val interceptors = listOf(interceptorProvider.serverErrorInterceptor)
        return getApiBuilder(
            appEnv,
            settingsProvider,
            interceptors,
        )
    }

    @RetrofitConfiguration(RetrofitConfigurations.BasicJson)
    @Provides
    @Singleton
    fun providesBasicJsonRetrofit(
        appEnv: AppEnv,
        settingsProvider: AppSettingsProvider,
        interceptorProvider: RetrofitInterceptorProvider,
        json: Json,
    ): Retrofit {
        val interceptors = listOf(interceptorProvider.serverErrorInterceptor)
        return getJsonApiBuilder(
            appEnv,
            settingsProvider,
            interceptors,
            json,
        )
    }

    /**
     * Since we're displaying SVGs in the app, Coil needs an ImageLoader which supports this
     * format. During Coil's initialization it will call `applicationContext.newImageLoader()` to
     * obtain an ImageLoader.
     *
     * @see <a href="https://github.com/coil-kt/coil/blob/main/coil-singleton/src/main/java/coil/Coil.kt">Coil</a>
     */
    @Provides
    @Singleton
    fun imageLoader(
        @ApplicationContext application: Context,
        appEnv: AppEnv,
    ) = ImageLoader.Builder(application)
        .components {
            add(SvgDecoder.Factory())
        }
        .diskCache {
            DiskCache.Builder()
                .directory(application.cacheDir.resolve("image_cache"))
                .maxSizePercent(0.02)
                .build()
        }
        // Assume most content images are versioned urls
        // but some problematic images are fetching each time
        .respectCacheHeaders(false)
        .apply {
            if (appEnv.isDebuggable) {
                val callFactory = OkHttpClient.Builder()
                    .addInterceptor(
                        HttpLoggingInterceptor()
                            .apply {
                                setLevel(HttpLoggingInterceptor.Level.BODY)
                            },
                    )
                    .build()
                callFactory(callFactory)
            }

            if (appEnv.isNotProduction) {
                logger(DebugLogger())
            }
        }
        .build()
}
