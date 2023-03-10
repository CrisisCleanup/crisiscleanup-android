package com.crisiscleanup.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.crisiscleanup.CrisisCleanupAppEnv
import com.crisiscleanup.core.appheader.AppHeaderUiState
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.NavigationObserver
import com.crisiscleanup.core.common.log.TagLogger
import com.crisiscleanup.core.network.RetrofitInterceptorProvider
import com.crisiscleanup.core.ui.SearchManager
import com.crisiscleanup.log.CrisisCleanupAppLogger
import com.crisiscleanup.navigation.CrisisCleanupNavigationObserver
import com.crisiscleanup.network.CrisisCleanupInterceptorProvider
import com.crisiscleanup.ui.AppSearchManager
import com.crisiscleanup.ui.CrisisCleanupAppHeaderUiState
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AppModule {
    @Singleton
    @Binds
    fun bindsCrisisCleanupAppEnv(appEnv: CrisisCleanupAppEnv): AppEnv

    @Binds
    fun bindsCrisisCleanupAppLogger(logger: CrisisCleanupAppLogger): TagLogger

    @Singleton
    @Binds
    fun bindsCrisisCleanupInterceptorProvider(
        provider: CrisisCleanupInterceptorProvider
    ): RetrofitInterceptorProvider

    @Singleton
    @Binds
    fun bindsAppHeaderUiState(headerUiState: CrisisCleanupAppHeaderUiState): AppHeaderUiState

    @Singleton
    @Binds
    fun bindsNavigationObserver(navigationObserver: CrisisCleanupNavigationObserver): NavigationObserver

    @Singleton
    @Binds
    fun bindSearchManager(searchManager: AppSearchManager): SearchManager
}

@Module
@InstallIn(SingletonComponent::class)
object AppObjectModule {
    @Singleton
    @Provides
    fun providesCredentialManager(
        @ApplicationContext context: Context,
    ) = CredentialManager.create(context)
}