package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.IncidentOrganizationEntity
import com.crisiscleanup.core.database.model.OrganizationPrimaryContactCrossRef
import com.crisiscleanup.core.network.model.NetworkIncidentOrganization

fun NetworkIncidentOrganization.asEntity() = IncidentOrganizationEntity(
    id = id,
    name = name,
)

fun NetworkIncidentOrganization.primaryContactCrossReferences() =
    primaryContacts.map { OrganizationPrimaryContactCrossRef(id, it.id) }