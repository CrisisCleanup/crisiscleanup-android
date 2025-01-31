package com.crisiscleanup.core.data.di

import com.crisiscleanup.core.data.AccountListsSyncer
import com.crisiscleanup.core.data.IncidentOrganizationsDataCache
import com.crisiscleanup.core.data.IncidentOrganizationsDataFileCache
import com.crisiscleanup.core.data.ListsSyncer
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
    fun bindsListsSyncer(syncer: AccountListsSyncer): ListsSyncer
}
