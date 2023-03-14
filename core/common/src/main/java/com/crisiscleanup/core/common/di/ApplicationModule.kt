package com.crisiscleanup.core.common.di

import com.crisiscleanup.core.common.*
import com.crisiscleanup.core.common.event.CrisisCleanupTrimMemoryEventManager
import com.crisiscleanup.core.common.event.TrimMemoryEventManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface ApplicationModule {
    @Singleton
    @Binds
    fun bindsAndroidResourceProvider(
        resourceProvider: ApplicationResourceProvider
    ): AndroidResourceProvider

    @Binds
    fun bindsInputValidator(
        validator: CommonInputValidator
    ): InputValidator

    @Singleton
    @Binds
    fun bindsTrimMemoryEventManager(
        manager: CrisisCleanupTrimMemoryEventManager
    ): TrimMemoryEventManager

    @Binds
    fun bindsMemoryStats(memoryStats: AndroidAppMemoryStats): AppMemoryStats

    @Binds
    fun bindsAppVersionProvider(
        versionProvider: AndroidAppVersionProvider
    ): AppVersionProvider
}