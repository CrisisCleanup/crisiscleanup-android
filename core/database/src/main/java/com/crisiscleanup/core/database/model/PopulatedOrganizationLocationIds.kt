package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo

data class PopulatedOrganizationLocationIds(
    @ColumnInfo("primary_location")
    val primary: Long?,
    @ColumnInfo("secondary_location")
    val secondary: Long?,
)
