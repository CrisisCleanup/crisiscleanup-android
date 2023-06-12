package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.WorksiteFlagEntity
import com.crisiscleanup.core.model.data.WorksiteFlagType
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import kotlinx.datetime.Clock

fun NetworkWorksiteFull.FlagShort.asEntity() = WorksiteFlagEntity(
    id = 0,
    networkId = -1,
    worksiteId = 0,
    action = null,
    createdAt = Clock.System.now(),
    isHighPriority = reasonT == WorksiteFlagType.HighPriority.literal,
    notes = null,
    reasonT = reasonT ?: "",
    requestedAction = null,
)
