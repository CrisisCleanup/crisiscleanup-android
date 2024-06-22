package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.ListEntity
import com.crisiscleanup.core.network.model.NetworkList

internal fun NetworkList.asEntity() = ListEntity(
    id = 0,
    networkId = id,
    localGlobalUuid = "",
    createdBy = createdBy,
    updatedBy = updatedBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    parent = parent,
    name = name,
    description = description,
    listOrder = listOrder,
    tags = tags,
    model = model,
    objectIds = (objectIds ?: emptyList()).joinToString(","),
    shared = shared,
    permissions = permissions,
    incidentId = incident,
)
