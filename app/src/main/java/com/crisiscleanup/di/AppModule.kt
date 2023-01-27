package com.crisiscleanup.di

import com.crisiscleanup.CrisisCleanupAppEnv
import com.crisiscleanup.core.appheader.AppHeaderUiState
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.network.RetrofitInterceptorProvider
import com.crisiscleanup.log.CrisisCleanupAppLogger
import com.crisiscleanup.network.CrisisCleanupInterceptorProvider
import com.crisiscleanup.ui.CrisisCleanupAppHeaderUiState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AppModule {
    @Singleton
    @Binds
    fun bindsCrisisCleanupAppEnv(appEnv: CrisisCleanupAppEnv): AppEnv

    @Singleton
    @Binds
    fun bindsCrisisCleanupAppLogger(logger: CrisisCleanupAppLogger): AppLogger

    @Singleton
    @Binds
    fun bindsCrisisCleanupInterceptorProvider(
        provider: CrisisCleanupInterceptorProvider
    ): RetrofitInterceptorProvider

    @Singleton
    @Binds
    fun bindsAppHeaderUiState(headerUiState: CrisisCleanupAppHeaderUiState): AppHeaderUiState
}