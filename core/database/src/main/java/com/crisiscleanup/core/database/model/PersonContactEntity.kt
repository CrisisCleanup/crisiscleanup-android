package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
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