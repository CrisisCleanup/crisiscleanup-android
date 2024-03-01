package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.network.CrisisCleanupAccountApi
import com.crisiscleanup.core.network.retrofit.DataApiClient
import kotlinx.coroutines.flow.first
import javax.inject.Inject

interface RequestRedeployRepository {
    suspend fun getRequestedIncidents(): Set<Long>
    suspend fun requestRedeploy(incidentId: Long): Boolean
}

class CrisisCleanupRequestRedeployRepository @Inject constructor(
    private val networkDataSource: DataApiClient,
    private val accountDataRepository: AccountDataRepository,
    private val accountApi: CrisisCleanupAccountApi,
    @Logger(CrisisCleanupLoggers.Account) private val logger: AppLogger,
) : RequestRedeployRepository {
    override suspend fun getRequestedIncidents(): Set<Long> {
        try {
// return networkDataSource.
        } catch (e: Exception) {
            logger.logException(e)
        }
        return emptySet()
    }

    override suspend fun requestRedeploy(incidentId: Long): Boolean {
        try {
            val organizationId = accountDataRepository.accountData.first().org.id
            return accountApi.requestRedeploy(organizationId, incidentId)
        } catch (e: Exception) {
            logger.logException(e)
        }
        return false
    }
}
