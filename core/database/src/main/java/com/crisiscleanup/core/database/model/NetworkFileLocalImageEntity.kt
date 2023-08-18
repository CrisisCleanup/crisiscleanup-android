package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    "network_file_local_images",
    foreignKeys = [
        ForeignKey(
            entity = NetworkFileEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("is_deleted"),
    ],
)
data class NetworkFileLocalImageEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo("is_deleted")
    val isDeleted: Boolean = false,
    @ColumnInfo("rotate_degrees")
    val rotateDegrees: Int = 0,
)
