package com.crisiscleanup.core.data

import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.model.asEntities
import com.crisiscleanup.core.data.util.IncidentDataPullStats
import com.crisiscleanup.core.data.util.IncidentDataPullStatsUpdater
import com.crisiscleanup.core.database.dao.IncidentOrganizationDao
import com.crisiscleanup.core.database.dao.IncidentOrganizationDaoPlus
import com.crisiscleanup.core.database.dao.PersonContactDao
import com.crisiscleanup.core.database.model.IncidentOrganizationSyncStatsEntity
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import javax.inject.Inject

interface OrganizationsSyncer {
    suspend fun sync(incidentId: Long)
}

// TODO Write tests

class IncidentOrganizationsSyncer @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val networkDataCache: IncidentOrganizationsDataCache,
    private val incidentOrganizationDao: IncidentOrganizationDao,
    private val incidentOrganizationDaoPlus: IncidentOrganizationDaoPlus,
    private val personContactDao: PersonContactDao,
    private val appVersionProvider: AppVersionProvider,
    @Logger(CrisisCleanupLoggers.Incidents) private val logger: AppLogger,
) : OrganizationsSyncer {
    val dataPullStats = MutableStateFlow(IncidentDataPullStats())

    override suspend fun sync(incidentId: Long) {
        val statsUpdater = IncidentDataPullStatsUpdater(
            updatePullStats = { stats -> dataPullStats.value = stats },
        ).also {
            it.beginPull(incidentId)
        }
        try {
            saveOrganizationsData(incidentId, statsUpdater)
        } finally {
            statsUpdater.endPull()
        }
    }

    private suspend fun saveOrganizationsData(
        incidentId: Long,
        statsUpdater: IncidentDataPullStatsUpdater,
    ) = coroutineScope {
        var syncCount = 100
        statsUpdater.updateDataCount(syncCount)

        statsUpdater.setPagingRequest()

        val syncStart = Clock.System.now()

        var requestedCount = 0
        var networkDataOffset = 0
        val pageDataCount = 200
        try {
            while (networkDataOffset < syncCount) {
                val worksitesRequest = networkDataSource.getIncidentOrganizations(
                    incidentId,
                    pageDataCount,
                    networkDataOffset,
                )

                syncCount = worksitesRequest.count ?: 0
                statsUpdater.updateDataCount(syncCount)

                worksitesRequest.results?.let {
                    networkDataCache.saveOrganizations(
                        incidentId,
                        networkDataOffset,
                        syncCount,
                        it,
                    )
                } ?: break

                networkDataOffset += pageDataCount

                ensureActive()

                requestedCount = networkDataOffset.coerceAtMost(syncCount)
                statsUpdater.updateRequestedCount(requestedCount)
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }

            logger.logException(e)
        }

        var dbSaveCount = 0
        for (dbSaveOffset in 0 until requestedCount step pageDataCount) {
            val cachedData = networkDataCache.loadOrganizations(
                incidentId,
                dbSaveOffset,
                syncCount,
            ) ?: break

            val entities = cachedData.organizations.asEntities(
                getContacts = true,
                getReferences = false,
            )
            val organizations = entities.organizations
            incidentOrganizationDaoPlus.saveOrganizations(
                organizations,
                entities.primaryContacts,
                entities.organizationIncidentLookup,
            )

            statsUpdater.addSavedCount((organizations.size * 0.5).toInt())

            dbSaveCount += pageDataCount
        }

        for (dbSaveOffset in 0 until requestedCount step pageDataCount) {
            val cachedData = networkDataCache.loadOrganizations(
                incidentId,
                dbSaveOffset,
                syncCount,
            ) ?: break

            val (
                organizations,
                _,
                organizationContactCrossRefs,
                organizationAffiliates,
            ) = cachedData.organizations.asEntities(getContacts = false, getReferences = true)
            incidentOrganizationDaoPlus.saveOrganizationReferences(
                organizations,
                organizationContactCrossRefs,
                organizationAffiliates,
            )

            statsUpdater.addSavedCount((organizations.size * 0.5).toInt())
        }

        if (dbSaveCount >= syncCount) {
            incidentOrganizationDao.upsertStats(
                IncidentOrganizationSyncStatsEntity(
                    incidentId = incidentId,
                    targetCount = syncCount,
                    successfulSync = syncStart,
                    appBuildVersionCode = appVersionProvider.versionCode,
                ),
            )
        }

        for (deleteCacheOffset in 0 until networkDataOffset step pageDataCount) {
            networkDataCache.deleteOrganizations(incidentId, deleteCacheOffset)
        }

        personContactDao.trimIncidentOrganizationContacts()
    }
}
