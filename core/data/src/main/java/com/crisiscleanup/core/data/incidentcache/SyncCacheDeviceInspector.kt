package com.crisiscleanup.core.data.incidentcache

import com.crisiscleanup.core.common.AppMemoryStats
import javax.inject.Inject
import javax.inject.Singleton

interface SyncCacheDeviceInspector {
    val isLimitedDevice: Boolean
}

@Singleton
class WorksitesSyncCacheDeviceInspector @Inject constructor(
    memoryStats: AppMemoryStats,
) : SyncCacheDeviceInspector {
    // TODO Account for other device properties like Wifi signal and reliability
    private val allWorksitesMemoryThreshold = 100
    override val isLimitedDevice = memoryStats.availableMemory < allWorksitesMemoryThreshold
}
