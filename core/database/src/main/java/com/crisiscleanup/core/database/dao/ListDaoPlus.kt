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

        for (l in upsertLists) {
            val insertId = listDao.insertIgnoreList(l)
            if (insertId < 0) {
                // TODO Do not update where local changes exist and were made after updatedAt
                listDao.syncUpdateList(
                    networkId = l.networkId,
                    updatedBy = l.updatedBy,
                    updatedAt = l.updatedAt,
                    parent = l.parent,
                    name = l.name,
                    description = l.description ?: "",
                    listOrder = l.listOrder,
                    tags = l.tags ?: "",
                    model = l.model,
                    objectIds = l.objectIds,
                    shared = l.shared,
                    permissions = l.permissions,
                    incident = l.incidentId,
                )
            }
        }

        listDao.deleteLists(deleteNetworkIds)
    }
}