package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.crisiscleanup.core.model.data.IncidentOrganization
import com.crisiscleanup.core.model.data.OrganizationIdName

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
        ),
    )
    val primaryContacts: List<PersonContactEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
    )
    val affiliateIds: List<OrganizationAffiliateEntity>,
)

fun PopulatedIncidentOrganization.asExternalModel() = with(entity) {
    IncidentOrganization(
        id = id,
        name = name,
        primaryContacts = primaryContacts.map(PersonContactEntity::asExternalModel),
        affiliateIds = mutableSetOf(id).apply {
            addAll(affiliateIds.map(OrganizationAffiliateEntity::affiliateId))
        },
    )
}

fun Collection<OrganizationIdName>.asLookup() = associate { it.id to it.name }
