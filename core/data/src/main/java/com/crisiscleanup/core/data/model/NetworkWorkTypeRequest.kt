package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.WorkTypeTransferRequestEntity
import com.crisiscleanup.core.network.model.NetworkWorkTypeRequest

fun NetworkWorkTypeRequest.asEntity(worksiteId: Long) = WorkTypeTransferRequestEntity(
    id = 0,
    networkId = id,
    worksiteId = worksiteId,
    workType = workType.workType,
    reason = "",
    byOrg = byOrg.id,
    toOrg = toOrg.id,
    createdAt = createdAt,
    approvedAt = approvedAt,
    rejectedAt = rejectedAt,
    approvedRejectedReason = acceptedRejectedReason ?: "",
)