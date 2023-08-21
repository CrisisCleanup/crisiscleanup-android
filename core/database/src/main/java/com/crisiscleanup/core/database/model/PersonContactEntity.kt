package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.crisiscleanup.core.model.data.PersonContact

@Entity("person_contacts")
data class PersonContactEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo("first_name")
    val firstName: String,
    @ColumnInfo("last_name")
    val lastName: String,
    val email: String,
    val mobile: String,
)

fun PersonContactEntity.asExternalModel() = PersonContact(
    id = id,
    firstName = firstName,
    lastName = lastName,
    email = email,
    mobile = mobile,
)

@Entity(
    "person_to_organization",
    primaryKeys = ["id", "organization_id"],
    foreignKeys = [
        ForeignKey(
            entity = PersonContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = IncidentOrganizationEntity::class,
            parentColumns = ["id"],
            childColumns = ["organization_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["organization_id", "id"]),
    ],
)
data class PersonOrganizationCrossRef(
    val id: Long,
    @ColumnInfo("organization_id")
    val organizationId: Long,
)
