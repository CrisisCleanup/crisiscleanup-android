package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.WorkTypeEntity
import javax.inject.Inject

class WorkTypeDaoPlus @Inject constructor(
    private val db: CrisisCleanupDatabase,
) {
    suspend fun syncUpsert(workTypes: List<WorkTypeEntity>) = db.withTransaction {
        val workTypeDao = db.workTypeDao()
        workTypes.forEach { workType ->
            val id = workTypeDao.insertIgnoreWorkType(workType)
            if (id < 0) {
                workTypeDao.syncUpdateWorkType(
                    worksiteId = workType.worksiteId,
                    networkId = workType.networkId,
                    orgClaim = workType.orgClaim,
                    status = workType.status,
                    workType = workType.workType,
                    createdAt = workType.createdAt,
                    nextRecurAt = workType.nextRecurAt,
                    phase = workType.phase,
                    recur = workType.recur,
                )
            }
        }
    }
}
