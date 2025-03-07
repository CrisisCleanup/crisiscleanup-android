package com.crisiscleanup.core.data.di

import com.crisiscleanup.core.data.incidentcache.DataDownloadSpeedMonitor
import com.crisiscleanup.core.data.incidentcache.IncidentDataDownloadSpeedMonitor
import com.crisiscleanup.core.data.incidentcache.IncidentDataPullReporter
import com.crisiscleanup.core.data.incidentcache.SyncCacheDeviceInspector
import com.crisiscleanup.core.data.incidentcache.WorksitesSyncCacheDeviceInspector
import com.crisiscleanup.core.data.repository.IncidentWorksitesCacheRepository
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
    fun bindsDataDownloadSpeedMonitor(monitor: IncidentDataDownloadSpeedMonitor): DataDownloadSpeedMonitor

    @Binds
    fun bindsIncidentDataPullReporter(reporter: IncidentWorksitesCacheRepository): IncidentDataPullReporter
}
