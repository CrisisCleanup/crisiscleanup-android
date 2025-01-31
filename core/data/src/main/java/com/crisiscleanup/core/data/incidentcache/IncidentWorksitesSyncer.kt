package com.crisiscleanup.core.data.incidentcache

import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers.Worksites
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.model.IncidentDataPullStats
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.dao.WorksiteSyncStatDao
import com.crisiscleanup.core.model.data.IncidentDataSyncStats
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant
import javax.inject.Inject

class IncidentWorksitesSyncer @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val worksiteDaoPlus: WorksiteDaoPlus,
    private val worksiteSyncStatDao: WorksiteSyncStatDao,
    private val deviceInspector: SyncCacheDeviceInspector,
    private val appVersionProvider: AppVersionProvider,
    @Logger(Worksites) private val logger: AppLogger,
) : WorksitesSyncer {
    override val dataPullStats = MutableStateFlow(IncidentDataPullStats())

    override suspend fun networkWorksitesCount(incidentId: Long, updatedAfter: Instant?) =
        networkDataSource.getWorksitesCount(incidentId, updatedAfter)

    override suspend fun sync(incidentId: Long, syncStats: IncidentDataSyncStats) {
        // TODO
    }
}
