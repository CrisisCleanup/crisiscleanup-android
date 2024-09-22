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
    @ColumnInfo(defaultValue = "")
    val profilePictureUri: String,
    @ColumnInfo(defaultValue = "")
    val activeRoles: String,
)

fun String.splitToInts() = split(",").mapNotNull { it.toIntOrNull() }

fun PersonContactEntity.asExternalModel() = PersonContact(
    id = id,
    firstName = firstName,
    lastName = lastName,
    email = email,
    mobile = mobile,
    profilePictureUri = profilePictureUri,
    activeRoles = activeRoles.splitToInts(),
)

fun Collection<PersonContactEntity>.asExternalModelSorted() =
    map(PersonContactEntity::asExternalModel)
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, PersonContact::fullName))

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

@Entity(
    "person_to_equipment",
    primaryKeys = ["id", "equipment_id"],
    foreignKeys = [
        ForeignKey(
            entity = PersonContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EquipmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipment_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["equipment_id", "id"],
            name = "idx_equipment_to_person",
        ),
    ],
)
data class PersonEquipmentCrossRef(
    @ColumnInfo("id")
    val id: Long,
    @ColumnInfo("equipment_id")
    val equipmentId: Long,
)
