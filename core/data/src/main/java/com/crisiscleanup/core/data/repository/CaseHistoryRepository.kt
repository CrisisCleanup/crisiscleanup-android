package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers.Cases
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.model.asEntities
import com.crisiscleanup.core.database.dao.CaseHistoryDao
import com.crisiscleanup.core.database.dao.CaseHistoryDaoPlus
import com.crisiscleanup.core.database.dao.PersonContactDao
import com.crisiscleanup.core.database.dao.WorksiteDao
import com.crisiscleanup.core.database.model.PopulatedCaseHistoryEvent
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.CaseHistoryEvent
import com.crisiscleanup.core.model.data.CaseHistoryUserEvents
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface CaseHistoryRepository {
    val loadingWorksiteId: Flow<Long>

    suspend fun streamEvents(worksiteId: Long): Flow<List<CaseHistoryUserEvents>>

    suspend fun refreshEvents(worksiteId: Long)
}

@Singleton
class OfflineFirstCaseHistoryRepository @Inject constructor(
    private val caseHistoryDao: CaseHistoryDao,
    private val personContactDao: PersonContactDao,
    private val worksiteDao: WorksiteDao,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val caseHistoryDaoPlus: CaseHistoryDaoPlus,
    @Logger(Cases) private val logger: AppLogger,
) : CaseHistoryRepository {
    private val _loadingWorksiteId = MutableStateFlow(EmptyWorksite.id)
    override val loadingWorksiteId = _loadingWorksiteId

    override suspend fun streamEvents(worksiteId: Long) = caseHistoryDao.streamEvents(worksiteId)
        .map { events ->
            val epoch0 = Instant.fromEpochSeconds(0)
            val userEventMap = mutableMapOf<Long, MutableList<CaseHistoryEvent>>()
            val userNewestCreatedAtMap = mutableMapOf<Long, Instant>()
            events.map(PopulatedCaseHistoryEvent::asExternalModel)
                .forEach { event ->
                    val userId = event.createdBy
                    if (!userEventMap.contains(userId)) {
                        userEventMap[userId] = mutableListOf()
                        userNewestCreatedAtMap[userId] = epoch0
                    }
                    userEventMap[userId]?.add(event)
                    if (event.createdAt > userNewestCreatedAtMap[userId]!!) {
                        userNewestCreatedAtMap[userId] = event.createdAt
                    }
                }

            // TODO Check cancellation
            // ensureActive()

            val sortingData = mutableListOf<Pair<CaseHistoryUserEvents, Instant>>()
            for ((userId, userEvents) in userEventMap) {
                val contact = personContactDao.getContact(userId)
                val person = contact?.entity
                val org = contact?.organization
                sortingData.add(
                    Pair(
                        CaseHistoryUserEvents(
                            userId = userId,
                            userName = "${person?.firstName ?: ""} ${person?.lastName ?: ""}".trim(),
                            orgName = org?.name ?: "",
                            userPhone = person?.mobile ?: "",
                            userEmail = person?.email ?: "",
                            events = userEvents,
                        ),
                        userNewestCreatedAtMap[userId] ?: epoch0,
                    )
                )
            }

            sortingData.sortedByDescending { it.second }
                .map { it.first }
        }

    override suspend fun refreshEvents(worksiteId: Long) {
        _loadingWorksiteId.value = worksiteId
        try {
            val networkWorksiteId = worksiteDao.getWorksiteNetworkId(worksiteId)
            val entities = networkDataSource.getCaseHistory(networkWorksiteId)
                .map { it.asEntities(worksiteId) }
            val events = entities.map { it.first }
            val attrs = entities.map { it.second }
            caseHistoryDaoPlus.saveEvents(events, attrs)
        } catch (e: Exception) {
            logger.logException(e)
        } finally {
            _loadingWorksiteId.value = EmptyWorksite.id
        }
    }
}