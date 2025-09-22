package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.database.dao.IncidentDao
import com.crisiscleanup.core.database.dao.IncidentDaoPlus
import com.crisiscleanup.core.datastore.AccountInfoDataSource
import com.crisiscleanup.core.model.data.IncidentClaimThreshold
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

interface IncidentClaimThresholdRepository {
    suspend fun saveIncidentClaimThresholds(
        accountId: Long,
        incidentThresholds: List<IncidentClaimThreshold>,
    )

    fun onWorksiteCreated(worksiteId: Long)
}

@Singleton
class CrisisCleanupIncidentClaimThresholdRepository @Inject constructor(
    incidentDao: IncidentDao,
    accountInfoDataSource: AccountInfoDataSource,
    private val incidentDaoPlus: IncidentDaoPlus,
    appConfigRepository: AppConfigRepository,
    incidentSelector: IncidentSelector,
    @Logger(CrisisCleanupLoggers.Incidents) private val logger: AppLogger,
) : IncidentClaimThresholdRepository {
    private val worksitesCreated = ConcurrentHashMap<Long, Boolean>()

    private val incidentClaimThresholdConfig = appConfigRepository.appConfig

    private val currentIncidentClaimThresholds = combine(
        accountInfoDataSource.accountData,
        incidentSelector.incidentId,
        ::Pair,
    )
        .flatMapLatest { (accountData, incidentId) ->
            val accountId = accountData.id
            incidentDao.streamIncidentClaimThreshold(
                accountId = accountId,
                incidentId = incidentId,
            )
                .mapNotNull { it }
                .map {
                    IncidentClaimThreshold(
                        incidentId = incidentId,
                        claimedCount = it.userClaimCount,
                        closedRatio = it.userCloseRatio,
                    )
                }
        }

    private val isAtIncidentClaimThreshold = combine(
        incidentClaimThresholdConfig,
        currentIncidentClaimThresholds,
        ::Pair,
    )
        .mapLatest { (thresholdConfig, currentClaims) ->
            currentClaims.claimedCount > thresholdConfig.claimCountThreshold &&
                currentClaims.closedRatio < thresholdConfig.closedClaimRatioThreshold
        }

    override suspend fun saveIncidentClaimThresholds(
        accountId: Long,
        incidentThresholds: List<IncidentClaimThreshold>,
    ) {
        try {
            incidentDaoPlus.saveIncidentThresholds(accountId, incidentThresholds)
        } catch (e: Exception) {
            logger.logException(e)
        }
    }

    override fun onWorksiteCreated(worksiteId: Long) {
        worksitesCreated.put(worksiteId, true)
    }
}
