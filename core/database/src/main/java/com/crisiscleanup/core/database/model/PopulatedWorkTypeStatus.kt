package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo

data class PopulatedWorkTypeStatus(
    val status: String,
    val name: String,
    @ColumnInfo("primary_state")
    val primaryState: String,
)

fun List<PopulatedWorkTypeStatus>.asStatusLookup() = associateBy(PopulatedWorkTypeStatus::status)
