package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.database.dao.WorkTypeStatusDao
import com.crisiscleanup.core.database.model.PopulatedWorkTypeStatus
import com.crisiscleanup.core.database.model.asStatusLookup
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.statusFromLiteral
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkWorkTypeStatusFull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

interface WorkTypeStatusRepository {
    val workTypeStatusOptions: StateFlow<List<WorkTypeStatus>>

    suspend fun loadStatuses(force: Boolean = false)

    fun translateStatus(status: String): String?

    fun translateStatus(status: WorkTypeStatus): String?
}

@Singleton
class CrisisCleanupWorkTypeStatusRepository @Inject constructor(
    private val workTypeStatusDao: WorkTypeStatusDao,
    private val networkClient: CrisisCleanupNetworkDataSource,
    private val networkMonitor: NetworkMonitor,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : WorkTypeStatusRepository {
    private var statusLookup: Map<String, PopulatedWorkTypeStatus> = emptyMap()

    override var workTypeStatusOptions = MutableStateFlow(emptyList<WorkTypeStatus>())

    override suspend fun loadStatuses(force: Boolean) {
        if (statusLookup.isNotEmpty()) {
            return
        }

        if (force || workTypeStatusDao.getCount() == 0) {
            try {
                if (networkMonitor.isOnline.first()) {
                    networkClient.getStatuses().results?.let {
                        workTypeStatusDao.upsert(it.map(NetworkWorkTypeStatusFull::asEntity))
                    }
                }
            } catch (e: Exception) {
                logger.logException(e)
            }
        }

        statusLookup = workTypeStatusDao.getStatuses().asStatusLookup()
        workTypeStatusOptions.value = statusLookup.filter { it.value.primaryState != "need" }
            .map { statusFromLiteral(it.key) }
    }

    override fun translateStatus(status: String) = statusLookup[status]?.name

    override fun translateStatus(status: WorkTypeStatus) = translateStatus(status.literal)
}
