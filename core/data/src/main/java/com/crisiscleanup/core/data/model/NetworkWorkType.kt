package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.WorkTypeEntity
import com.crisiscleanup.core.network.model.NetworkWorkType
import com.crisiscleanup.core.network.model.NetworkWorksiteFull.WorkTypeShort

fun NetworkWorkType.asEntity() = WorkTypeEntity(
    id = 0,
    localGlobalUuid = "",
    // Incoming network ID is always defined
    networkId = id!!,
    worksiteId = 0,
    createdAt = createdAt,
    orgClaim = orgClaim,
    nextRecurAt = nextRecurAt,
    phase = phase,
    recur = recur,
    status = status,
    workType = workType,
)

fun WorkTypeShort.asEntity() = WorkTypeEntity(
    id = 0,
    localGlobalUuid = "",
    networkId = id,
    worksiteId = 0,
    createdAt = null,
    orgClaim = orgClaim,
    nextRecurAt = null,
    phase = null,
    recur = null,
    status = status,
    workType = workType,
)