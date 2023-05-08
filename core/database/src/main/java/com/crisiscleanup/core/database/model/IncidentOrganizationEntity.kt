package com.crisiscleanup.core.database.model

import androidx.room.*

// Changes below should update IncidentOrganizationsStableModelBuildVersion in core.network

@Entity("incident_organizations")
data class IncidentOrganizationEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
)

@Entity(
    "organization_to_primary_contact",
    primaryKeys = ["organization_id", "contact_id"],
    foreignKeys = [
        ForeignKey(
            entity = IncidentOrganizationEntity::class,
            parentColumns = ["id"],
            childColumns = ["organization_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PersonContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contact_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["contact_id", "organization_id"],
            name = "idx_contact_to_organization",
        ),
    ]
)
data class OrganizationPrimaryContactCrossRef(
    @ColumnInfo("organization_id")
    val organizationId: Long,
    @ColumnInfo("contact_id")
    val contactId: Long,
)

@Entity(
    "organization_to_affiliate",
    primaryKeys = ["id", "affiliate_id"],
    foreignKeys = [
        ForeignKey(
            entity = IncidentOrganizationEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = IncidentOrganizationEntity::class,
            parentColumns = ["id"],
            childColumns = ["affiliate_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["affiliate_id"]),
    ]
)
data class OrganizationAffiliateEntity(
    val id: Long,
    @ColumnInfo("affiliate_id")
    val affiliateId: Long
)