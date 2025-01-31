package com.crisiscleanup.core.data.di

import com.crisiscleanup.core.data.IncidentWorksitesFullSyncer
import com.crisiscleanup.core.data.IncidentWorksitesSecondaryDataSyncer
import com.crisiscleanup.core.data.IncidentWorksitesSyncer
import com.crisiscleanup.core.data.WorksitesFullSyncer
import com.crisiscleanup.core.data.WorksitesNetworkDataCache
import com.crisiscleanup.core.data.WorksitesNetworkDataFileCache
import com.crisiscleanup.core.data.WorksitesSecondaryDataSyncer
import com.crisiscleanup.core.data.incidentcache.SyncCacheDeviceInspector
import com.crisiscleanup.core.data.incidentcache.WorksitesSyncCacheDeviceInspector
import com.crisiscleanup.core.data.incidentcache.WorksitesSyncer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface IncidentCacheModule {
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
}
