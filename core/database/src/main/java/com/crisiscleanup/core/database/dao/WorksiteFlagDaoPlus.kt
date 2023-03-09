package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.WorksiteFlagEntity
import javax.inject.Inject

class WorksiteFlagDaoPlus @Inject constructor(
    private val db: CrisisCleanupDatabase,
) {
    suspend fun syncUpsert(workTypes: List<WorksiteFlagEntity>) = db.withTransaction {
        val flagDao = db.worksiteFlagDao()
        workTypes.forEach { flag ->
            val id = flagDao.insertIgnoreFlag(flag)
            if (id < 0) {
                flagDao.syncUpdateFlag(
                    worksiteId = flag.worksiteId,
                    networkId = flag.networkId,
                    action = flag.action,
                    createdAt = flag.createdAt,
                    isHighPriority = flag.isHighPriority,
                    notes = flag.notes,
                    reasonT = flag.reasonT,
                    requestedAction = flag.requestedAction,
                )
            }
        }
    }
}
