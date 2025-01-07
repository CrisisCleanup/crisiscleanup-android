package com.crisiscleanup.core.data.di

import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.data.AccountListsSyncer
import com.crisiscleanup.core.data.IncidentOrganizationsDataCache
import com.crisiscleanup.core.data.IncidentOrganizationsDataFileCache
import com.crisiscleanup.core.data.IncidentWorksitesFullSyncer
import com.crisiscleanup.core.data.IncidentWorksitesSecondaryDataSyncer
import com.crisiscleanup.core.data.IncidentWorksitesSyncer
import com.crisiscleanup.core.data.ListsSyncer
import com.crisiscleanup.core.data.SyncCacheDeviceInspector
import com.crisiscleanup.core.data.WorksitesFullSyncer
import com.crisiscleanup.core.data.WorksitesNetworkDataCache
import com.crisiscleanup.core.data.WorksitesNetworkDataFileCache
import com.crisiscleanup.core.data.WorksitesSecondaryDataSyncer
import com.crisiscleanup.core.data.WorksitesSyncCacheDeviceInspector
import com.crisiscleanup.core.data.WorksitesSyncer
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.AccountUpdateRepository
import com.crisiscleanup.core.data.repository.AppDataManagementRepository
import com.crisiscleanup.core.data.repository.AppEndOfLifeRepository
import com.crisiscleanup.core.data.repository.AppMetricsRepository
import com.crisiscleanup.core.data.repository.AppMetricsRepositoryImpl
import com.crisiscleanup.core.data.repository.AppPreferencesRepository
import com.crisiscleanup.core.data.repository.AppPreferencesRepositoryImpl
import com.crisiscleanup.core.data.repository.CaseHistoryRepository
import com.crisiscleanup.core.data.repository.CrisisCleanupAccountDataRepository
import com.crisiscleanup.core.data.repository.CrisisCleanupAccountUpdateRepository
import com.crisiscleanup.core.data.repository.CrisisCleanupDataManagementRepository
import com.crisiscleanup.core.data.repository.CrisisCleanupEquipmentRepository
import com.crisiscleanup.core.data.repository.CrisisCleanupListsRepository
import com.crisiscleanup.core.data.repository.CrisisCleanupLocalImageRepository
import com.crisiscleanup.core.data.repository.CrisisCleanupOrgVolunteerRepository
import com.crisiscleanup.core.data.repository.CrisisCleanupRequestRedeployRepository
import com.crisiscleanup.core.data.repository.CrisisCleanupShareLocationRepository
import com.crisiscleanup.core.data.repository.CrisisCleanupTeamChangeRepository
import com.crisiscleanup.core.data.repository.CrisisCleanupTeamsRepository
import com.crisiscleanup.core.data.repository.CrisisCleanupWorkTypeStatusRepository
import com.crisiscleanup.core.data.repository.CrisisCleanupWorksiteChangeRepository
import com.crisiscleanup.core.data.repository.EndOfLifeRepository
import com.crisiscleanup.core.data.repository.EquipmentRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LanguageTranslationsRepository
import com.crisiscleanup.core.data.repository.ListsRepository
import com.crisiscleanup.core.data.repository.LocalImageRepository
import com.crisiscleanup.core.data.repository.LocationsRepository
import com.crisiscleanup.core.data.repository.MemoryCacheSearchWorksitesRepository
import com.crisiscleanup.core.data.repository.OfflineFirstCaseHistoryRepository
import com.crisiscleanup.core.data.repository.OfflineFirstIncidentsRepository
import com.crisiscleanup.core.data.repository.OfflineFirstLanguageTranslationsRepository
import com.crisiscleanup.core.data.repository.OfflineFirstLocationsRepository
import com.crisiscleanup.core.data.repository.OfflineFirstOrganizationsRepository
import com.crisiscleanup.core.data.repository.OfflineFirstUsersRepository
import com.crisiscleanup.core.data.repository.OfflineFirstWorksiteImageRepository
import com.crisiscleanup.core.data.repository.OfflineFirstWorksitesRepository
import com.crisiscleanup.core.data.repository.OrgVolunteerRepository
import com.crisiscleanup.core.data.repository.OrganizationsRepository
import com.crisiscleanup.core.data.repository.RequestRedeployRepository
import com.crisiscleanup.core.data.repository.SearchWorksitesRepository
import com.crisiscleanup.core.data.repository.ShareLocationRepository
import com.crisiscleanup.core.data.repository.TeamChangeRepository
import com.crisiscleanup.core.data.repository.TeamsRepository
import com.crisiscleanup.core.data.repository.UsersRepository
import com.crisiscleanup.core.data.repository.WorkTypeStatusRepository
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksiteImageRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.data.util.ConnectivityManagerNetworkMonitor
import com.crisiscleanup.core.data.util.IncidentDataPullReporter
import com.crisiscleanup.core.data.util.TimeZoneBroadcastMonitor
import com.crisiscleanup.core.data.util.TimeZoneMonitor
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
        networkMonitor: ConnectivityManagerNetworkMonitor,
    ): NetworkMonitor

    @Binds
    fun bindsTimeZoneMonitor(monitor: TimeZoneBroadcastMonitor): TimeZoneMonitor

    @Singleton
    @Binds
    fun bindsLocalAppPreferencesRepository(
        repository: AppPreferencesRepositoryImpl,
    ): AppPreferencesRepository

    @Singleton
    @Binds
    fun bindsLocalAppMetricsRepository(
        repository: AppMetricsRepositoryImpl,
    ): AppMetricsRepository

    @Singleton
    @Binds
    fun bindsAccountDataRepository(
        repository: CrisisCleanupAccountDataRepository,
    ): AccountDataRepository

    @Singleton
    @Binds
    fun bindsWorkTypeStatusRepository(
        repository: CrisisCleanupWorkTypeStatusRepository,
    ): WorkTypeStatusRepository

    @Singleton
    @Binds
    fun bindsIncidentsRepository(
        incidentsRepository: OfflineFirstIncidentsRepository,
    ): IncidentsRepository

    @Singleton
    @Binds
    fun bindsLocationRepository(
        locationsRepository: OfflineFirstLocationsRepository,
    ): LocationsRepository

    @Singleton
    @Binds
    fun bindsWorksiteRepository(
        worksitesRepository: OfflineFirstWorksitesRepository,
    ): WorksitesRepository

    @Singleton
    @Binds
    fun bindsWorksitesDataPullReporter(
        reporter: OfflineFirstWorksitesRepository,
    ): IncidentDataPullReporter

    @Singleton
    @Binds
    fun bindsLanguageTranslationRepository(
        translationsRepository: OfflineFirstLanguageTranslationsRepository,
    ): LanguageTranslationsRepository

    @Singleton
    @Binds
    fun bindsKeyTranslator(
        translator: OfflineFirstLanguageTranslationsRepository,
    ): KeyTranslator

    @Binds
    fun bindsSearchWorksitesRepository(
        repository: MemoryCacheSearchWorksitesRepository,
    ): SearchWorksitesRepository

    @Singleton
    @Binds
    fun bindsWorksiteChangeRepository(
        repository: CrisisCleanupWorksiteChangeRepository,
    ): WorksiteChangeRepository

    @Singleton
    @Binds
    fun bindsOrganizationsRepository(
        repository: OfflineFirstOrganizationsRepository,
    ): OrganizationsRepository

    @Binds
    @Singleton
    fun bindsLocalImageRepository(
        repository: CrisisCleanupLocalImageRepository,
    ): LocalImageRepository

    @Binds
    fun bindsAppDataManagementRepository(
        repository: CrisisCleanupDataManagementRepository,
    ): AppDataManagementRepository

    @Binds
    fun bindsUsersRepository(repository: OfflineFirstUsersRepository): UsersRepository

    @Singleton
    @Binds
    fun bindsCaseHistoryRepository(
        repository: OfflineFirstCaseHistoryRepository,
    ): CaseHistoryRepository

    @Binds
    fun bindsAccountUpdateRepository(
        repository: CrisisCleanupAccountUpdateRepository,
    ): AccountUpdateRepository

    @Binds
    fun bindsEndOfLifeRepository(repository: AppEndOfLifeRepository): EndOfLifeRepository

    @Binds
    fun bindsOrgVolunteerRepository(
        repository: CrisisCleanupOrgVolunteerRepository,
    ): OrgVolunteerRepository

    @Binds
    fun bindsRequestRedeployRepository(
        repository: CrisisCleanupRequestRedeployRepository,
    ): RequestRedeployRepository

    @Binds
    fun bindsWorksiteImageRepository(
        repository: OfflineFirstWorksiteImageRepository,
    ): WorksiteImageRepository

    @Binds
    fun bindsListRepository(repository: CrisisCleanupListsRepository): ListsRepository

    @Binds
    fun bindsTeamsRepository(repository: CrisisCleanupTeamsRepository): TeamsRepository

    @Binds
    fun bindsShareLocationRepository(
        repository: CrisisCleanupShareLocationRepository,
    ): ShareLocationRepository

    @Binds
    fun bindsEquipmentRepository(
        repository: CrisisCleanupEquipmentRepository,
    ): EquipmentRepository

    @Singleton
    @Binds
    fun bindsTeamChangeRepository(
        repository: CrisisCleanupTeamChangeRepository,
    ): TeamChangeRepository
}

@Module
@InstallIn(SingletonComponent::class)
interface DataInternalModule {
    @Binds
    fun bindsSyncCacheDeviceInspector(inspector: WorksitesSyncCacheDeviceInspector): SyncCacheDeviceInspector

    @Binds
    fun bindsWorksitesNetworkDataCache(cache: WorksitesNetworkDataFileCache): WorksitesNetworkDataCache

    @Binds
    fun bindsWorksitesSyncer(syncer: IncidentWorksitesSyncer): WorksitesSyncer

    @Binds
    fun bindsWorksitesFullSyncer(syncer: IncidentWorksitesFullSyncer): WorksitesFullSyncer

    @Binds
    fun bindsWorksitesSecondaryDataSyncer(syncer: IncidentWorksitesSecondaryDataSyncer): WorksitesSecondaryDataSyncer

    @Binds
    fun providesIncidentOrganizationsNetworkDataCache(
        cache: IncidentOrganizationsDataFileCache,
    ): IncidentOrganizationsDataCache

    @Binds
    fun bindsListsSyncer(syncer: AccountListsSyncer): ListsSyncer
}
