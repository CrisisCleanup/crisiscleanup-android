package com.crisiscleanup.core.data.di

import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.CrisisCleanupAccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.data.repository.LocationsRepository
import com.crisiscleanup.core.data.repository.OfflineFirstIncidentsRepository
import com.crisiscleanup.core.data.repository.OfflineFirstLocalAppPreferencesRepository
import com.crisiscleanup.core.data.repository.OfflineFirstLocationsRepository
import com.crisiscleanup.core.data.repository.OfflineFirstWorksitesRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
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
}
