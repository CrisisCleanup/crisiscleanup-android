package com.crisiscleanup.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.split
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.database.dao.ListDao
import com.crisiscleanup.core.database.dao.ListDaoPlus
import com.crisiscleanup.core.database.model.ListEntity
import com.crisiscleanup.core.database.model.PopulatedList
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.CrisisCleanupList
import com.crisiscleanup.core.model.data.EmptyList
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.CrisisCleanupNetworkException
import com.crisiscleanup.core.network.model.NetworkList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface ListsRepository {
    fun streamIncidentLists(incidentId: Long): Flow<List<CrisisCleanupList>>

    fun pageLists(): Flow<PagingData<CrisisCleanupList>>

    fun streamList(listId: Long): Flow<CrisisCleanupList>

    suspend fun syncLists(lists: List<NetworkList>)

    suspend fun refreshList(id: Long)
}

class CrisisCleanupListsRepository @Inject constructor(
    private val listDao: ListDao,
    private val listDaoPlus: ListDaoPlus,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
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

    private fun syncUpdateList(list: ListEntity) = with(list) {
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

    override suspend fun refreshList(id: Long) {
        listDao.getList(id)?.let { cachedList ->
            if (cachedList.networkId > 0) {
                // TODO Skip update where locally modified
                try {
                    networkDataSource.getList(cachedList.networkId)?.asEntity()?.let { updateList ->
                        syncUpdateList(updateList)
                    }
                } catch (e: Exception) {
                    (e as? CrisisCleanupNetworkException)?.statusCode?.let { code ->
                        if (code == 404) {
                            // TODO Handle. Delete outright or show as deleted on backend?
                            logger.logDebug("List does not exist on backend. Delete and take additional action")
                            return
                        }
                    }
                    logger.logException(e)
                }
            }
        }
    }
}