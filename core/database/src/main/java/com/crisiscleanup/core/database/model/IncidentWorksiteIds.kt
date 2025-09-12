package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo

// Used as db model and external model
// Names must remain consistent
data class IncidentWorksiteIds(
    @ColumnInfo("incident_id")
    val incidentId: Long,
    @ColumnInfo("id")
    val worksiteId: Long,
    @ColumnInfo("network_id")
    val networkWorksiteId: Long,
)
