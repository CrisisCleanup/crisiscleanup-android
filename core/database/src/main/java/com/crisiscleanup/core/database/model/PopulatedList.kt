package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.crisiscleanup.core.model.data.CrisisCleanupList
import com.crisiscleanup.core.model.data.EmptyList
import com.crisiscleanup.core.model.data.IncidentIdNameType
import com.crisiscleanup.core.model.data.listModelFromLiteral
import com.crisiscleanup.core.model.data.listPermissionFromLiteral
import com.crisiscleanup.core.model.data.listShareFromLiteral

data class PopulatedList(
    @Embedded
    val entity: ListEntity,
    @Relation(
        parentColumn = "incident_id",
        entityColumn = "id",
    )
    val incident: IncidentEntity?,
)

fun PopulatedList.asExternalModel() = with(entity) {
    val numericObjectIds = objectIds.trim().split(",")
        .mapNotNull { it.trim().toLongOrNull() }
    CrisisCleanupList(
        id = id,
        updatedAt = updatedAt,
        networkId = networkId,
        parentNetworkId = parent,
        name = name,
        description = description ?: "",
        listOrder = listOrder,
        tags = tags,
        model = listModelFromLiteral(model),
        objectIds = numericObjectIds,
        shared = listShareFromLiteral(shared),
        permission = listPermissionFromLiteral(permissions),
        incident = incident?.let {
            IncidentIdNameType(
                id = incident.id,
                name = incident.name,
                shortName = incident.shortName,
                disasterLiteral = incident.type,
            )
        } ?: EmptyList.incident,
    )
}
