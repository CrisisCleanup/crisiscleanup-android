package com.crisiscleanup.core.network.di

import android.content.Context
import com.crisiscleanup.core.network.AccessTokenManager
import com.crisiscleanup.core.network.SimpleAccessTokenManager
import com.crisiscleanup.core.network.fake.FakeAssetManager
import dagger.Binds
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
    fun providesNetworkJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun providesFakeAssetManager(
        @ApplicationContext context: Context,
    ): FakeAssetManager = FakeAssetManager(context.assets::open)
}

@Module
@InstallIn(SingletonComponent::class)
interface BindingNetworkModule {
    @Binds
    @Singleton
    fun bindsAccessTokenManager(
        accessTokenManager: SimpleAccessTokenManager
    ): AccessTokenManager
}
