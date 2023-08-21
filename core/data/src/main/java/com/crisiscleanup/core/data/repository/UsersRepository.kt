package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.model.asExternalModel
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkPersonContact
import javax.inject.Inject

interface UsersRepository {
    suspend fun getMatchingUsers(
        q: String,
        organization: Long,
        limit: Int = 10,
    ): List<PersonContact>
}

class OfflineFirstUsersRepository @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : UsersRepository {
    override suspend fun getMatchingUsers(
        q: String,
        organization: Long,
        limit: Int,
    ): List<PersonContact> {
        try {
            return networkDataSource.searchUsers(q, organization, limit)
                .map(NetworkPersonContact::asExternalModel)
        } catch (e: Exception) {
            logger.logException(e)
        }
        return emptyList()
    }
}
