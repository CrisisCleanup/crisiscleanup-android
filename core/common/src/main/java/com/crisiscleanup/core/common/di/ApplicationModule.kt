package com.crisiscleanup.core.common.di

import com.crisiscleanup.core.common.AndroidAppMemoryStats
import com.crisiscleanup.core.common.AndroidAppVersionProvider
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.AndroidResourceTranslator
import com.crisiscleanup.core.common.AppIncidentMapTracker
import com.crisiscleanup.core.common.AppMemoryStats
import com.crisiscleanup.core.common.AppSettingsProvider
import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.ApplicationResourceProvider
import com.crisiscleanup.core.common.CommonInputValidator
import com.crisiscleanup.core.common.IncidentMapTracker
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.SecretsAppSettingsProvider
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

    @Singleton
    @Binds
    fun bindsIncidentMapTracker(tracker: AppIncidentMapTracker): IncidentMapTracker
}
