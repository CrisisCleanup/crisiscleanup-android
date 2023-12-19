package com.crisiscleanup.core.common.di

import com.crisiscleanup.core.common.*
import com.crisiscleanup.core.common.event.CrisisCleanupExternalEventBus
import com.crisiscleanup.core.common.event.CrisisCleanupTrimMemoryEventManager
import com.crisiscleanup.core.common.event.ExternalEventBus
import com.crisiscleanup.core.common.event.TrimMemoryEventManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface ApplicationModule {
    @Binds
    fun bindsSettingsProvider(
        provider: SecretsAppSettingsProvider,
    ): AppSettingsProvider

    @Singleton
    @Binds
    fun bindsAndroidResourceProvider(
        resourceProvider: ApplicationResourceProvider,
    ): AndroidResourceProvider

    @Binds
    fun bindsInputValidator(
        validator: CommonInputValidator,
    ): InputValidator

    @Singleton
    @Binds
    fun bindsTrimMemoryEventManager(
        manager: CrisisCleanupTrimMemoryEventManager,
    ): TrimMemoryEventManager

    @Binds
    fun bindsMemoryStats(memoryStats: AndroidAppMemoryStats): AppMemoryStats

    @Binds
    fun bindsAppVersionProvider(
        versionProvider: AndroidAppVersionProvider,
    ): AppVersionProvider

    @Binds
    fun bindsTranslator(
        translator: AndroidResourceTranslator,
    ): KeyResourceTranslator

    @Singleton
    @Binds
    fun bindsExternalEventBus(
        bus: CrisisCleanupExternalEventBus,
    ): ExternalEventBus
}
