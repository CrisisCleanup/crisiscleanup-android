package com.crisiscleanup.di

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import com.crisiscleanup.AndroidLocationProvider
import com.crisiscleanup.AndroidPermissionManager
import com.crisiscleanup.AppVisualAlertManager
import com.crisiscleanup.CrisisCleanupAppEnv
import com.crisiscleanup.core.appheader.AppHeaderUiState
import com.crisiscleanup.core.common.*
import com.crisiscleanup.core.common.log.TagLogger
import com.crisiscleanup.core.network.AuthInterceptorProvider
import com.crisiscleanup.core.network.RetrofitInterceptorProvider
import com.crisiscleanup.log.CrisisCleanupAppLogger
import com.crisiscleanup.navigation.CrisisCleanupNavigationObserver
import com.crisiscleanup.network.CrisisCleanupAuthInterceptorProvider
import com.crisiscleanup.network.CrisisCleanupInterceptorProvider
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
    fun bindsCrisisCleanupAuthInterceptorProvider(
        provider: CrisisCleanupAuthInterceptorProvider,
    ): AuthInterceptorProvider

    @Singleton
    @Binds
    fun bindsCrisisCleanupInterceptorProvider(
        provider: CrisisCleanupInterceptorProvider,
    ): RetrofitInterceptorProvider

    @Singleton
    @Binds
    fun bindsAppHeaderUiState(headerUiState: CrisisCleanupAppHeaderUiState): AppHeaderUiState

    @Singleton
    @Binds
    fun bindsNavigationObserver(navigationObserver: CrisisCleanupNavigationObserver): NavigationObserver

    @Singleton
    @Binds
    fun bindsPermissionManager(manager: AndroidPermissionManager): PermissionManager

    @Binds
    fun bindsLocationProvider(provider: AndroidLocationProvider): LocationProvider

    @Binds
    fun bindsVisualAlertManager(manager: AppVisualAlertManager): VisualAlertManager
}

@Module
@InstallIn(SingletonComponent::class)
object AppObjectModule {
    @Provides
    fun providesPackageManager(
        @ApplicationContext context: Context,
    ): PackageManager = context.packageManager

    @Provides
    fun providesContentResolver(
        @ApplicationContext context: Context,
    ): ContentResolver = context.contentResolver
}
