package com.crisiscleanup.core.data

import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.model.affiliateOrganizationCrossReferences
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.data.model.primaryContactCrossReferences
import com.crisiscleanup.core.data.util.IncidentDataPullStats
import com.crisiscleanup.core.data.util.IncidentDataPullStatsUpdater
import com.crisiscleanup.core.database.dao.IncidentOrganizationDao
import com.crisiscleanup.core.database.dao.IncidentOrganizationDaoPlus
import com.crisiscleanup.core.database.dao.PersonContactDao
import com.crisiscleanup.core.database.model.IncidentOrganizationSyncStatsEntity
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkCrisisCleanupApiError
import com.crisiscleanup.core.network.model.NetworkIncidentOrganization
import com.crisiscleanup.core.network.model.NetworkPersonContact
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
    private val authEventManager: AuthEventManager,
    private val appVersionProvider: AppVersionProvider,
    @Logger(CrisisCleanupLoggers.Incidents) private val logger: AppLogger,
) : OrganizationsSyncer {
    val dataPullStats = MutableStateFlow(IncidentDataPullStats())

    override suspend fun sync(incidentId: Long) {
        val statsUpdater = IncidentDataPullStatsUpdater(
            updatePullStats = { stats -> dataPullStats.value = stats }
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
                NetworkCrisisCleanupApiError.tryThrowException(
                    authEventManager,
                    worksitesRequest.errors
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
            if (e is InterruptedException) {
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

            val organizations = cachedData.organizations.map { it.asEntity() }
            val primaryContacts =
                cachedData.organizations.flatMap { it.primaryContacts.map(NetworkPersonContact::asEntity) }
            val organizationContactCrossRefs =
                cachedData.organizations.flatMap(NetworkIncidentOrganization::primaryContactCrossReferences)
            val organizationAffiliates =
                cachedData.organizations.flatMap(NetworkIncidentOrganization::affiliateOrganizationCrossReferences)
            incidentOrganizationDaoPlus.saveOrganizations(
                organizations,
                primaryContacts,
                organizationContactCrossRefs,
                organizationAffiliates,
            )

            statsUpdater.addSavedCount(organizations.size)

            dbSaveCount += pageDataCount
        }

        if (dbSaveCount >= syncCount) {
            incidentOrganizationDao.upsertStats(
                IncidentOrganizationSyncStatsEntity(
                    incidentId = incidentId,
                    targetCount = syncCount,
                    successfulSync = syncStart,
                    appBuildVersionCode = appVersionProvider.versionCode,
                )
            )
        }

        for (deleteCacheOffset in 0 until networkDataOffset step pageDataCount) {
            networkDataCache.deleteOrganizations(incidentId, deleteCacheOffset)
        }

        personContactDao.trimIncidentOrganizationContacts()
    }
}