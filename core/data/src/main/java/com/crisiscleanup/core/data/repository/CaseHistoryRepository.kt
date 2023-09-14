package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers.Cases
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.model.PersonContactEntities
import com.crisiscleanup.core.data.model.asEntities
import com.crisiscleanup.core.database.dao.CaseHistoryDao
import com.crisiscleanup.core.database.dao.CaseHistoryDaoPlus
import com.crisiscleanup.core.database.dao.IncidentOrganizationDaoPlus
import com.crisiscleanup.core.database.dao.PersonContactDao
import com.crisiscleanup.core.database.dao.PersonContactDaoPlus
import com.crisiscleanup.core.database.dao.WorksiteDao
import com.crisiscleanup.core.database.model.PopulatedCaseHistoryEvent
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.CaseHistoryEvent
import com.crisiscleanup.core.model.data.CaseHistoryUserEvents
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkPersonContact
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface CaseHistoryRepository {
    val loadingWorksiteId: Flow<Long>

    suspend fun streamEvents(worksiteId: Long): Flow<List<CaseHistoryUserEvents>>

    suspend fun refreshEvents(worksiteId: Long): Int
}

@Singleton
class OfflineFirstCaseHistoryRepository @Inject constructor(
    private val caseHistoryDao: CaseHistoryDao,
    private val personContactDao: PersonContactDao,
    private val worksiteDao: WorksiteDao,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val caseHistoryDaoPlus: CaseHistoryDaoPlus,
    private val personContactDaoPlus: PersonContactDaoPlus,
    private val incidentOrganizationDaoPlus: IncidentOrganizationDaoPlus,
    private val translator: LanguageTranslationsRepository,
    @Logger(Cases) private val logger: AppLogger,
) : CaseHistoryRepository {
    private val refreshingWorksiteEvents = MutableStateFlow(EmptyWorksite.id)
    private val loadingWorksiteEvents = MutableStateFlow(EmptyWorksite.id)
    override val loadingWorksiteId = combine(
        refreshingWorksiteEvents,
        loadingWorksiteEvents,
    ) { id0, id1 ->
        if (id0 == EmptyWorksite.id) id1 else id0
    }

    override suspend fun streamEvents(worksiteId: Long) = caseHistoryDao.streamEvents(worksiteId)
        .map { events ->
            loadingWorksiteEvents.value = worksiteId
            try {
                loadEvents(events)
            } finally {
                loadingWorksiteEvents.value = EmptyWorksite.id
            }
        }

    private suspend fun loadEvents(
        events: List<PopulatedCaseHistoryEvent>,
    ) = coroutineScope {
        val epoch0 = Instant.fromEpochSeconds(0)
        val userEventMap = mutableMapOf<Long, MutableList<CaseHistoryEvent>>()
        val userNewestCreatedAtMap = mutableMapOf<Long, Instant>()
        events.map { it.asExternalModel(translator) }
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

        ensureActive()

        val userIds = userEventMap.keys
        queryUpdateUsers(userIds)

        ensureActive()

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
                ),
            )
        }

        sortingData.sortedByDescending { it.second }
            .map { it.first }
    }

    private suspend fun queryUpdateUsers(userIds: Collection<Long>) {
        try {
            val networkUsers = networkDataSource.getUsers(userIds)
            val entities = networkUsers.mapNotNull(NetworkPersonContact::asEntities)

            val organizations = entities.map(PersonContactEntities::organization)
            val affiliates = entities.map(PersonContactEntities::organizationAffiliates)
            incidentOrganizationDaoPlus.saveMissing(organizations, affiliates)

            val persons = entities.map(PersonContactEntities::personContact)
            val personOrganizations = entities.map(PersonContactEntities::personToOrganization)
            personContactDaoPlus.savePersons(persons, personOrganizations)
        } catch (e: Exception) {
            logger.logException(e)
        }
    }

    override suspend fun refreshEvents(worksiteId: Long): Int {
        refreshingWorksiteEvents.value = worksiteId
        try {
            val networkWorksiteId = worksiteDao.getWorksiteNetworkId(worksiteId)
            val entities = networkDataSource.getCaseHistory(networkWorksiteId)
                .map { it.asEntities(worksiteId) }
            val events = entities.map { it.first }
            val attrs = entities.map { it.second }
            caseHistoryDaoPlus.saveEvents(worksiteId, events, attrs)
            return events.size
        } catch (e: Exception) {
            logger.logException(e)
        } finally {
            refreshingWorksiteEvents.value = EmptyWorksite.id
        }
        return 0
    }
}
