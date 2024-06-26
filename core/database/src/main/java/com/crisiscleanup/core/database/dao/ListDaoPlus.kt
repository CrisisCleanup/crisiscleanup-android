package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.ListEntity
import javax.inject.Inject

class ListDaoPlus @Inject constructor(
    private val db: CrisisCleanupDatabase,
) {
    suspend fun syncUpdateLists(
        upsertLists: List<ListEntity>,
        deleteNetworkIds: Set<Long>,
    ) = db.withTransaction {
        val listDao = db.listDao()

        for (list in upsertLists) {
            val insertId = listDao.insertIgnoreList(list)
            if (insertId < 0) {
                // TODO Do not update where local changes exist and were made after updatedAt
                with(list) {
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
        }

        listDao.deleteListsByNetworkIds(deleteNetworkIds)
    }
}
