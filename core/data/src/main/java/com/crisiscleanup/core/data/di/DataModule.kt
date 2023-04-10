package com.crisiscleanup.core.data.di

import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.data.IncidentWorksitesSyncer
import com.crisiscleanup.core.data.WorksitesNetworkDataCache
import com.crisiscleanup.core.data.WorksitesNetworkDataFileCache
import com.crisiscleanup.core.data.WorksitesSyncer
import com.crisiscleanup.core.data.repository.*
import com.crisiscleanup.core.data.util.ConnectivityManagerNetworkMonitor
import com.crisiscleanup.core.data.util.NetworkMonitor
import com.crisiscleanup.core.data.util.WorksitesDataPullReporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {
    @Singleton
    @Binds
    fun bindsNetworkMonitor(
        networkMonitor: ConnectivityManagerNetworkMonitor
    ): NetworkMonitor

    @Singleton
    @Binds
    fun bindsLocalAppPreferencesRepository(
        repository: OfflineFirstLocalAppPreferencesRepository
    ): LocalAppPreferencesRepository

    @Singleton
    @Binds
    fun bindsAccountDataRepository(
        repository: CrisisCleanupAccountDataRepository
    ): AccountDataRepository

    @Singleton
    @Binds
    fun bindsIncidentsRepository(
        incidentsRepository: OfflineFirstIncidentsRepository
    ): IncidentsRepository

    @Singleton
    @Binds
    fun bindsLocationRepository(
        locationsRepository: OfflineFirstLocationsRepository
    ): LocationsRepository

    @Singleton
    @Binds
    fun bindsWorksiteRepository(
        worksitesRepository: OfflineFirstWorksitesRepository
    ): WorksitesRepository

    @Singleton
    @Binds
    fun bindsWorksitesDataPullReporter(
        reporter: OfflineFirstWorksitesRepository
    ): WorksitesDataPullReporter

    @Singleton
    @Binds
    fun bindsLanguageTranslationRepository(
        translationsRepository: OfflineFirstLanguageTranslationsRepository
    ): LanguageTranslationsRepository

    @Singleton
    @Binds
    fun bindsKeyTranslator(
        translator: OfflineFirstLanguageTranslationsRepository
    ): KeyTranslator

    @Binds
    fun bindsSearchWorksitesRepository(
        repository: MemoryCacheSearchWorksitesRepository
    ): SearchWorksitesRepository

    @Binds
    fun bindsWorksiteChangeRepository(
        repository: CrisisCleanupWorksiteChangeRepository
    ): WorksiteChangeRepository
}

@Module
@InstallIn(SingletonComponent::class)
interface DataInternalModule {
    @Binds
    fun providesWorksitesNetworkDataCache(
        cache: WorksitesNetworkDataFileCache
    ): WorksitesNetworkDataCache

    @Binds
    fun providesWorksitesSyncer(syncer: IncidentWorksitesSyncer): WorksitesSyncer
}