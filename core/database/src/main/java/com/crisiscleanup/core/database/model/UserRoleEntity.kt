package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("user_roles")
data class UserRoleEntity(
    @PrimaryKey
    val id: Int,
    @ColumnInfo("name_key")
    val nameKey: String,
    @ColumnInfo("description_key")
    val descriptionKey: String,
    val level: Int,
)
