package com.crisiscleanup.core.data.di

import com.crisiscleanup.core.data.AccountListsDataSyncer
import com.crisiscleanup.core.data.ListsDataSyncer
import com.crisiscleanup.core.data.incidentcache.IncidentOrganizationsDataCache
import com.crisiscleanup.core.data.incidentcache.IncidentOrganizationsDataFileCache
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DataSyncerModule {
    @Binds
    fun providesIncidentOrganizationsDataCache(
        cache: IncidentOrganizationsDataFileCache,
    ): IncidentOrganizationsDataCache

    @Binds
    fun bindsListsSyncer(syncer: AccountListsDataSyncer): ListsDataSyncer
}
