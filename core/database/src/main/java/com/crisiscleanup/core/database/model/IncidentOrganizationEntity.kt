package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Changes below should update IncidentOrganizationsStableModelBuildVersion in core.network

@Entity("incident_organizations")
data class IncidentOrganizationEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    @ColumnInfo("primary_location")
    val primaryLocation: Long?,
    @ColumnInfo("secondary_location")
    val secondaryLocation: Long?,
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
    ],
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
        // Do not key affiliate_id to IncidentOrganizationEntity::class as this requires
        // all (affiliate) organizations exist at time of insert
    ],
    indices = [
        Index(value = ["affiliate_id", "id"]),
    ],
)
data class OrganizationAffiliateEntity(
    val id: Long,
    @ColumnInfo("affiliate_id")
    val affiliateId: Long,
)

@Entity(
    "organization_to_incident",
    primaryKeys = ["id", "incident_id"],
    foreignKeys = [
        ForeignKey(
            entity = IncidentOrganizationEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
        ),
        // No foreign key to Incidents as older incidents may not have been cached
    ],
    indices = [
        Index(value = ["incident_id", "id"]),
    ],
)
data class OrganizationIncidentCrossRef(
    val id: Long,
    @ColumnInfo("incident_id")
    val incidentId: Long,
)
