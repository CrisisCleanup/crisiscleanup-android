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
            value = PersonOrganizationCrossRef::class,
            parentColumn = "id",
            entityColumn = "organization_id",
        )
    )
    // Missing cross ref will crash so be flexible
    val organization: IncidentOrganizationEntity?,
)
