package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.WorkTypeStatusEntity
import com.crisiscleanup.core.network.model.NetworkWorkTypeStatusFull

fun NetworkWorkTypeStatusFull.asEntity() = WorkTypeStatusEntity(
    status = status,
    name = name,
    primaryState = primaryState,
    listOrder = listOrder,
)
