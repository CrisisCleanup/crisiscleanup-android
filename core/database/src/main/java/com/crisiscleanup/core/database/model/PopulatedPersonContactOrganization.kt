package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.crisiscleanup.core.model.data.OrganizationIdName
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.core.model.data.PersonOrganization

data class PopulatedPersonContactOrganization(
    @Embedded
    val entity: PersonContactEntity,
    // Missing cross ref will crash so be flexible
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PersonOrganizationCrossRef::class,
            parentColumn = "id",
            entityColumn = "organization_id",
        ),
    )
    val organization: IncidentOrganizationEntity?,
)

data class PopulatedPersonContactMatch(
    val id: Long,
    @ColumnInfo("first_name")
    val firstName: String,
    @ColumnInfo("last_name")
    val lastName: String,
    val email: String,
    val mobile: String,
    @ColumnInfo("profile_picture")
    val profilePictureUri: String,
    @ColumnInfo("active_roles")
    val activeRoles: String,
    @ColumnInfo("organization_id")
    val organizationId: Long,
    @ColumnInfo("organization_name")
    val organizationName: String,
)

fun PopulatedPersonContactMatch.asExternalModel() = PersonOrganization(
    PersonContact(
        id,
        firstName = firstName,
        lastName = lastName,
        email = email,
        mobile = mobile,
        profilePictureUri = profilePictureUri,
        fallbackAvatarUrl = fallbackAvatarUrl(firstName, lastName),
        activeRoles = activeRoles.splitToInts(),
    ),
    OrganizationIdName(
        organizationId,
        organizationName,
    ),
)
