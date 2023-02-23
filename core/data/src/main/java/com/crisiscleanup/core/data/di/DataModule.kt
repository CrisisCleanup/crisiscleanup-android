package com.crisiscleanup.core.data.di

import android.content.Context
import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.IncidentWorksitesDataManager
import com.crisiscleanup.core.data.repository.*
import com.crisiscleanup.core.data.util.ConnectivityManagerNetworkMonitor
import com.crisiscleanup.core.data.util.NetworkMonitor
import com.crisiscleanup.core.data.util.WorksitesDataPullReporter
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
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

@Module
@InstallIn(SingletonComponent::class)
object DataModuleObject {
    @Singleton
    @Provides
    fun providesIncidentWorksitesDataManager(
        networkDataSource: CrisisCleanupNetworkDataSource,
        authEventManager: AuthEventManager,
        @ApplicationContext context: Context,
        @Dispatcher(CrisisCleanupDispatchers.IO) ioDispatcher: CoroutineDispatcher,
        @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
    ) = IncidentWorksitesDataManager(
        networkDataSource, authEventManager, context, ioDispatcher, logger
    )
}