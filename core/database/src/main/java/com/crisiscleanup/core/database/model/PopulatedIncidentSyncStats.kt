package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Relation

data class PopulatedIncidentSyncStats(
    @Embedded
    val entity: WorksiteSyncStatsEntity,
    @Relation(
        parentColumn = "incident_id",
        entityColumn = "incident_id",
    )
    val fullStats: IncidentWorksitesFullSyncStatsEntity?,
    @Relation(
        parentColumn = "incident_id",
        entityColumn = "incident_id",
    )
    val secondaryStats: IncidentWorksitesSecondarySyncStatsEntity?,
) {
    val hasSyncedCore = with(entity) {
        successfulSync != null && pagedCount >= targetCount
    }
}
