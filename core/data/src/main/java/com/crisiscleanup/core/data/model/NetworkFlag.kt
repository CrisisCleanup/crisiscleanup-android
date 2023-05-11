package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.WorksiteFlagEntity
import com.crisiscleanup.core.model.data.HIGH_PRIORITY_FLAG
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import kotlinx.datetime.Clock

fun NetworkWorksiteFull.FlagShort.asEntity() = WorksiteFlagEntity(
    id = 0,
    networkId = -1,
    worksiteId = 0,
    action = null,
    createdAt = Clock.System.now(),
    isHighPriority = isHighPriority == true || reasonT == HIGH_PRIORITY_FLAG,
    notes = null,
    reasonT = reasonT ?: "",
    requestedAction = null,
)
