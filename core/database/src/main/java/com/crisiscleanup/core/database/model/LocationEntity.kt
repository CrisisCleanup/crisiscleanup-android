package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    "locations",
)
data class LocationEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo("shape_type", defaultValue = "")
    val shapeType: String,
    // Newline delimited sequences of
    //   comma delimited latitude,longitude coordinates
    @ColumnInfo(defaultValue = "")
    val coordinates: String,
)
