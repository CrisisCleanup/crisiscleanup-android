package com.crisiscleanup.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.split
import com.crisiscleanup.core.data.model.asEntities
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.database.dao.IncidentDao
import com.crisiscleanup.core.database.dao.IncidentOrganizationDao
import com.crisiscleanup.core.database.dao.IncidentOrganizationDaoPlus
import com.crisiscleanup.core.database.dao.ListDao
import com.crisiscleanup.core.database.dao.ListDaoPlus
import com.crisiscleanup.core.database.dao.PersonContactDao
import com.crisiscleanup.core.database.dao.WorksiteDao
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.model.PopulatedIncident
import com.crisiscleanup.core.database.model.PopulatedIncidentOrganization
import com.crisiscleanup.core.database.model.PopulatedList
import com.crisiscleanup.core.database.model.PopulatedWorksite
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.CrisisCleanupList
import com.crisiscleanup.core.model.data.EmptyList
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.IncidentOrganization
import com.crisiscleanup.core.model.data.ListModel
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.CrisisCleanupNetworkException
import com.crisiscleanup.core.network.model.NetworkIncidentOrganization
import com.crisiscleanup.core.network.model.NetworkList
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import javax.inject.Inject

interface ListsRepository {
    fun streamIncidentLists(incidentId: Long): Flow<List<CrisisCleanupList>>

    fun getIncidentListCount(incidentId: Long): Int

    fun pageLists(): Flow<PagingData<CrisisCleanupList>>

    fun streamList(listId: Long): Flow<CrisisCleanupList>

    suspend fun syncLists(lists: List<NetworkList>)

    suspend fun refreshList(id: Long)

    suspend fun getListObjectData(list: CrisisCleanupList): Map<Long, Any>
}

class CrisisCleanupListsRepository @Inject constructor(
    private val listDao: ListDao,
    private val listDaoPlus: ListDaoPlus,
    private val incidentDao: IncidentDao,
    private val organizationDao: IncidentOrganizationDao,
    private val incidentsRepository: IncidentsRepository,
    private val organizationDaoPlus: IncidentOrganizationDaoPlus,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val personContactDao: PersonContactDao,
    private val usersRepository: UsersRepository,
    private val worksiteDao: WorksiteDao,
    private val worksiteDaoPlus: WorksiteDaoPlus,
    @Logger(CrisisCleanupLoggers.Lists) private val logger: AppLogger,
) : ListsRepository {
    private val listPager = Pager(
        config = PagingConfig(pageSize = 30),
        pagingSourceFactory = {
            listDao.pageLists()
        },
    )

    override fun streamIncidentLists(incidentId: Long) =
        listDao.streamIncidentLists(incidentId).map { it.map(PopulatedList::asExternalModel) }

    override fun getIncidentListCount(incidentId: Long) = listDao.getIncidentListCount(incidentId)

    override fun pageLists() = listPager.flow.map {
        it.map(PopulatedList::asExternalModel)
    }

    override fun streamList(listId: Long) =
        listDao.streamList(listId).map { it?.asExternalModel() ?: EmptyList }

    override suspend fun syncLists(lists: List<NetworkList>) {
        val (validLists, invalidLists) = lists.split {
            it.invalidateAt == null
        }

        val listEntities = validLists.map(NetworkList::asEntity)
        val invalidNetworkIds = invalidLists.map(NetworkList::id).toSet()
        listDaoPlus.syncUpdateLists(listEntities, invalidNetworkIds)
    }

    override suspend fun refreshList(id: Long) {
        listDao.getList(id)?.let { cachedList ->
            if (cachedList.networkId > 0) {
                // TODO Skip update where locally modified
                //      How to handle delete where update exists? Should delete for consistency.
                try {
                    networkDataSource.getList(cachedList.networkId)
                        ?.asEntity()
                        ?.let {
                            with(it) {
                                listDao.syncUpdateList(
                                    networkId = networkId,
                                    updatedBy = updatedBy,
                                    updatedAt = updatedAt,
                                    parent = parent,
                                    name = name,
                                    description = description ?: "",
                                    listOrder = listOrder,
                                    tags = tags ?: "",
                                    model = model,
                                    objectIds = objectIds,
                                    shared = shared,
                                    permissions = permissions,
                                    incident = incidentId,
                                )
                            }
                        }
                } catch (e: Exception) {
                    (e as? CrisisCleanupNetworkException)?.statusCode?.let { code ->
                        if (code == 404) {
                            listDao.deleteList(id)
                            return
                        }
                    }
                    logger.logException(e)
                }
            }
        }
    }

    override suspend fun getListObjectData(list: CrisisCleanupList): Map<Long, Any> {
        if (list.incidentId > 0 && list.incident == null) {
            incidentsRepository.pullIncident(list.incidentId)
        }

        val objectIds = list.objectIds.toSet()

        when (list.model) {
            ListModel.Incident -> {
                val incidents = incidentDao.getIncidents(objectIds)
                    .map(PopulatedIncident::asExternalModel)
                return incidents.associateBy(Incident::id)
            }

            ListModel.List -> {
                fun getListLookup() = listDao.getListsByNetworkIds(objectIds)
                    .map(PopulatedList::asExternalModel)
                    .associateBy(CrisisCleanupList::networkId)

                var listLookup = getListLookup()
                if (listLookup.size != objectIds.size) {
                    val networkListIds = objectIds.filter { !listLookup.containsKey(it) }
                        // Guard against infinite refresh
                        .filter { it != list.networkId }
                    try {
                        val listEntities = networkDataSource.getLists(networkListIds)
                            .mapNotNull { it?.asEntity() }
                        listDaoPlus.syncUpdateLists(listEntities, emptySet())

                        listLookup = getListLookup()
                    } catch (e: Exception) {
                        logger.logException(e)
                    }
                }
                return listLookup
            }

            ListModel.Organization -> {
                fun getOrganizationLookup() = organizationDao.getOrganizations(objectIds)
                    .map(PopulatedIncidentOrganization::asExternalModel)
                    .associateBy(IncidentOrganization::id)

                var organizationLookup = getOrganizationLookup()
                if (organizationLookup.size != objectIds.size) {
                    val networkOrgIds = objectIds.filter { !organizationLookup.containsKey(it) }
                    try {
                        val organizationEntities = networkDataSource.getOrganizations(networkOrgIds)
                            .map(NetworkIncidentOrganization::asEntity)
                        organizationDaoPlus.saveOrganizations(
                            organizationEntities,
                            // TODO Save contacts and related data from network data. See IncidentOrganizationsSyncer for reference.
                            emptyList(),
                        )

                        organizationLookup = getOrganizationLookup()
                    } catch (e: Exception) {
                        logger.logException(e)
                    }
                }
                return organizationLookup
            }

            ListModel.User -> {
                fun getContactLookup() = personContactDao.getContacts(objectIds)
                    .map { it.entity.asExternalModel() }
                    .associateBy(PersonContact::id)

                var contactLookup = getContactLookup()
                if (contactLookup.size != objectIds.size) {
                    val userIds = objectIds.filter { !contactLookup.containsKey(it) }
                    try {
                        usersRepository.queryUpdateUsers(userIds)
                        contactLookup = getContactLookup()
                    } catch (e: Exception) {
                        logger.logException(e)
                    }
                }
                return contactLookup
            }

            ListModel.Worksite -> {
                fun getNetworkWorksiteLookup() = worksiteDao.getWorksitesByNetworkId(objectIds)
                    .map(PopulatedWorksite::asExternalModel)
                    .filter { it.networkId > 0 }
                    .associateBy(Worksite::networkId)

                var networkWorksiteLookup = getNetworkWorksiteLookup()
                if (networkWorksiteLookup.size != objectIds.size) {
                    val worksiteIds = objectIds.filter { !networkWorksiteLookup.containsKey(it) }
                    try {
                        val syncedAt = Clock.System.now()
                        networkDataSource.getWorksites(worksiteIds)?.let { networkWorksites ->
                            val entities = networkWorksites
                                .map(NetworkWorksiteFull::asEntities)
                            worksiteDaoPlus.syncWorksites(entities, syncedAt)
                        }

                        networkWorksiteLookup = getNetworkWorksiteLookup()
                    } catch (e: Exception) {
                        logger.logException(e)
                    }
                }
                return networkWorksiteLookup
            }

            else -> {}
        }

        return emptyMap()
    }
}
