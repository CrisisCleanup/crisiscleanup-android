package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.WorkTypeEntity
import com.crisiscleanup.core.network.model.NetworkWorksiteFull.WorkType
import com.crisiscleanup.core.network.model.NetworkWorksiteFull.WorkTypeShort

fun WorkType.asEntity() = WorkTypeEntity(
    id = 0,
    localGlobalUuid = "",
    networkId = id,
    worksiteId = 0,
    createdAt = createdAt,
    orgClaim = orgClaim,
    nextRecurAt = nextRecurAt,
    phase = phase,
    recur = recur,
    status = status,
    workType = workType,
    isInvalid = false,
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
    isInvalid = false,
)