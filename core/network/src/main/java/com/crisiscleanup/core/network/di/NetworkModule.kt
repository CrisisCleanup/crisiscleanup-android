package com.crisiscleanup.core.network.di

import android.content.Context
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.util.DebugLogger
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.network.BuildConfig
import com.crisiscleanup.core.network.RetrofitInterceptorProvider
import com.crisiscleanup.core.network.fake.FakeAssetManager
import com.crisiscleanup.core.network.retrofit.CrisisCleanupRetrofit
import com.crisiscleanup.core.network.retrofit.RequestHeaderKeysLookup
import com.crisiscleanup.core.network.retrofit.getCrisisCleanupApiBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
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

    @CrisisCleanupRetrofit
    @Provides
    @Singleton
    fun providesCrisisCleanupRetrofit(
        interceptorProvider: RetrofitInterceptorProvider,
        headerKeysLookup: RequestHeaderKeysLookup,
        json: Json,
        appEnv: AppEnv,
    ) = getCrisisCleanupApiBuilder(interceptorProvider, headerKeysLookup, json, appEnv)

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
    ): ImageLoader {
        val callFactory = OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor()
                    .apply {
                        if (BuildConfig.DEBUG) {
                            setLevel(HttpLoggingInterceptor.Level.BODY)
                        }
                    },
            )
            .build()

        return ImageLoader.Builder(application)
            .callFactory(callFactory)
            .components {
                add(SvgDecoder.Factory())
            }
            // Assume most content images are versioned urls
            // but some problematic images are fetching each time
            .respectCacheHeaders(false)
            .apply {
                if (appEnv.isNotProduction) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}
