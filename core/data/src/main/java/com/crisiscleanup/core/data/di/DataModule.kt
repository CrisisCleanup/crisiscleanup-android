package com.crisiscleanup.core.data.di

import com.crisiscleanup.core.data.IncidentSelectManager
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.CrisisCleanupAccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.data.repository.OfflineFirstIncidentsRepository
import com.crisiscleanup.core.data.repository.OfflineFirstLocalAppPreferencesRepository
import com.crisiscleanup.core.data.util.ConnectivityManagerNetworkMonitor
import com.crisiscleanup.core.data.util.NetworkMonitor
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
    fun bindsNetworkMonitor(
        networkMonitor: ConnectivityManagerNetworkMonitor
    ): NetworkMonitor

    @Singleton
    @Binds
    fun bindsIncidentsRepository(
        incidentsRepository: OfflineFirstIncidentsRepository
    ): IncidentsRepository

    @Singleton
    @Binds
    fun bindsIncidentSelector(selector: IncidentSelectManager): IncidentSelector
}
