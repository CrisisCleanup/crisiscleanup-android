package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PopulatedPersonContactOrganization(
    @Embedded
    val entity: PersonContactEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = OrganizationPrimaryContactCrossRef::class,
            parentColumn = "contact_id",
            entityColumn = "organization_id",
        )
    )
    val organization: IncidentOrganizationEntity,
)
