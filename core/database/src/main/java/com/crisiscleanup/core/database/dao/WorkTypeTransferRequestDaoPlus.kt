package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.WorkTypeTransferRequestEntity
import javax.inject.Inject

class WorkTypeTransferRequestDaoPlus @Inject constructor(
    private val db: CrisisCleanupDatabase,
) {
    suspend fun syncUpsert(requests: List<WorkTypeTransferRequestEntity>) {
        requests.firstOrNull()?.let {
            val worksiteId = it.worksiteId
            db.withTransaction {
                val transferRequestDao = db.workTypeTransferRequestDao()
                transferRequestDao.deleteUnspecified(
                    worksiteId,
                    requests.map(WorkTypeTransferRequestEntity::workType).toSet(),
                )
                requests.forEach { request ->
                    val id = transferRequestDao.insertIgnoreRequest(request)
                    if (id < 0) {
                        transferRequestDao.syncUpdateRequest(
                            worksiteId = request.worksiteId,
                            networkId = request.networkId,
                            workType = request.workType,
                            byOrg = request.byOrg,
                            toOrg = request.toOrg,
                            createdAt = request.createdAt,
                            approvedAt = request.approvedAt,
                            rejectedAt = request.rejectedAt,
                            approvedRejectedReason = request.approvedRejectedReason,
                        )
                    }
                }
            }
        }
    }
}
