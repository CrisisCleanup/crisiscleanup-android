package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Relation

data class PopulatedRecentWorksite(
    @Embedded
    val entity: RecentWorksiteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
    )
    val worksite: WorksiteEntity
)
