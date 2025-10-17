package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.ClaimCloseCounts
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.WorkTypeAnalyzer
import com.crisiscleanup.core.database.dao.IncidentDao
import com.crisiscleanup.core.database.dao.IncidentDaoPlus
import com.crisiscleanup.core.datastore.AccountInfoDataSource
import com.crisiscleanup.core.model.data.IncidentClaimThreshold
import com.crisiscleanup.core.model.data.IncidentsData
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

interface IncidentClaimThresholdRepository {
    suspend fun saveIncidentClaimThresholds(
        accountId: Long,
        incidentThresholds: List<IncidentClaimThreshold>,
    )

    fun onWorksiteCreated(worksiteId: Long)

    suspend fun isWithinClaimCloseThreshold(worksiteId: Long, additionalClaimCount: Int): Boolean
}

@Singleton
class CrisisCleanupIncidentClaimThresholdRepository @Inject constructor(
    private val incidentDao: IncidentDao,
    private val incidentDaoPlus: IncidentDaoPlus,
    private val accountInfoDataSource: AccountInfoDataSource,
    private val workTypeAnalyzer: WorkTypeAnalyzer,
    private val appConfigRepository: AppConfigRepository,
    private val incidentSelector: IncidentSelector,
    @Logger(CrisisCleanupLoggers.Incidents) private val logger: AppLogger,
) : IncidentClaimThresholdRepository {
    private val worksitesCreated = ConcurrentHashMap<Long, Boolean>()

    override fun onWorksiteCreated(worksiteId: Long) {
        worksitesCreated.put(worksiteId, true)
    }

    override suspend fun saveIncidentClaimThresholds(
        accountId: Long,
        incidentThresholds: List<IncidentClaimThreshold>,
    ) {
        try {
            val incidentsData = incidentSelector.data.value
            val incidentIds = (incidentsData as? IncidentsData.Incidents)?.incidents
                ?.map { it.id }
                ?.toSet()
                ?: emptySet()
            val thresholds = incidentThresholds.filter { incidentIds.contains(it.incidentId) }
            incidentDaoPlus.saveIncidentThresholds(accountId, thresholds)
        } catch (e: Exception) {
            logger.logException(e)
        }
    }

    override suspend fun isWithinClaimCloseThreshold(
        worksiteId: Long,
        additionalClaimCount: Int,
    ): Boolean {
        if (additionalClaimCount <= 0) {
            return true
        }

        val incidentId = incidentSelector.incidentId.first()

        val accountData = accountInfoDataSource.accountData.first()
        val accountId = accountData.id

        val thresholdConfig = appConfigRepository.appConfig.first()
        val claimCountThreshold = thresholdConfig.claimCountThreshold
        val closeRatioThreshold = thresholdConfig.closedClaimRatioThreshold

        val currentIncidentThreshold = incidentDao.getIncidentClaimThreshold(
            accountId = accountId,
            incidentId = incidentId,
        )
        val userClaimCount = currentIncidentThreshold?.userClaimCount ?: 0
        val userCloseRatio = currentIncidentThreshold?.userCloseRatio ?: 0.0f

        var unsyncedCounts = ClaimCloseCounts(0, 0)
        if (!worksitesCreated.containsKey(worksiteId)) {
            try {
                val orgId = accountData.org.id
                unsyncedCounts = workTypeAnalyzer.countUnsyncedClaimCloseWork(
                    orgId = orgId,
                    incidentId = incidentId,
                    worksitesCreated.keys,
                )
            } catch (e: Exception) {
                logger.logException(e)
            }
        }
        val unsyncedClaimCount = unsyncedCounts.claimCount

        val claimCount = userClaimCount + unsyncedClaimCount
        val closeRatio = if (claimCount > 0) {
            val userCloseCount = ceil(userCloseRatio * userClaimCount)
            val closeCount = userCloseCount + unsyncedCounts.closeCount
            closeCount / claimCount
        } else {
            userCloseRatio
        }

        return claimCount < claimCountThreshold ||
            closeRatio >= closeRatioThreshold
    }
}
