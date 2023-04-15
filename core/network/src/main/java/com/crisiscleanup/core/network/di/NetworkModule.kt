package com.crisiscleanup.core.network.di

import android.content.Context
import com.crisiscleanup.core.common.AppEnv
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
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun providesNetworkJson() = Json { ignoreUnknownKeys = true }

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
}
