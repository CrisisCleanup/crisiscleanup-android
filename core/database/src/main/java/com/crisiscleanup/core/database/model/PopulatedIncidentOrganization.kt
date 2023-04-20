package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.crisiscleanup.core.model.data.IncidentOrganization

data class PopulatedIncidentOrganization(
    @Embedded
    val entity: IncidentOrganizationEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = OrganizationPrimaryContactCrossRef::class,
            parentColumn = "organization_id",
            entityColumn = "contact_id",
        )
    )
    val primaryContacts: List<PersonContactEntity>
)

fun PopulatedIncidentOrganization.asExternalModel() = IncidentOrganization(
    id = entity.id,
    name = entity.name,
    primaryContacts = primaryContacts.map(PersonContactEntity::asExternalModel)
)

data class OrganizationIdName(
    val id: Long,
    val name: String,
)

fun Collection<OrganizationIdName>.asLookup() = associate { it.id to it.name }