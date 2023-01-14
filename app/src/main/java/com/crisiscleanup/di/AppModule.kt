package com.crisiscleanup.di

import com.crisiscleanup.CrisisCleanupAppEnv
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.network.RetrofitInterceptorProvider
import com.crisiscleanup.log.CrisisCleanupAppLogger
import com.crisiscleanup.network.CrisisCleanupHeaderInterceptorProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AppModule {
    @Binds
    fun bindsCrisisCleanupAppEnv(appEnv: CrisisCleanupAppEnv): AppEnv

    @Binds
    fun bindsCrisisCleanupAppLogger(logger: CrisisCleanupAppLogger): AppLogger

    @Binds
    fun bindsCrisisCleanupHeaderInterceptorProvider(
        provider: CrisisCleanupHeaderInterceptorProvider
    ): RetrofitInterceptorProvider
}