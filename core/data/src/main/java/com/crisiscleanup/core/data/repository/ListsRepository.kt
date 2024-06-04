package com.crisiscleanup.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.crisiscleanup.core.common.split
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.database.dao.ListDao
import com.crisiscleanup.core.database.dao.ListDaoPlus
import com.crisiscleanup.core.database.model.PopulatedList
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.CrisisCleanupList
import com.crisiscleanup.core.model.data.EmptyList
import com.crisiscleanup.core.network.model.NetworkList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface ListsRepository {
    fun streamIncidentLists(incidentId: Long): Flow<List<CrisisCleanupList>>

    fun pageLists(): Flow<PagingData<CrisisCleanupList>>

    fun streamList(listId: Long): Flow<CrisisCleanupList>

    suspend fun syncLists(lists: List<NetworkList>)
}

class CrisisCleanupListsRepository @Inject constructor(
    private val listDao: ListDao,
    private val listDaoPlus: ListDaoPlus,
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
}