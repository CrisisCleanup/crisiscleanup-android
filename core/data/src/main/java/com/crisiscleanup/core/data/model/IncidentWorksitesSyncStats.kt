package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.IncidentWorksitesSyncStatsEntity
import com.crisiscleanup.core.model.data.IncidentWorksitesSyncStats
import kotlinx.serialization.json.Json

fun IncidentWorksitesSyncStatsEntity.asExternalModel(): IncidentWorksitesSyncStats {
    val boundedParameters = try {
        Json.decodeFromString<IncidentWorksitesSyncStats.SyncBoundedParameters>(boundedParameters)
    } catch (e: Exception) {
        null
    }
    return IncidentWorksitesSyncStats(
        incidentId = id,
        syncTimestamps = IncidentWorksitesSyncStats.SyncTimestamps(
            before = updatedBefore,
            after = updatedAfter,
        ),
        fullSyncTimestamps = IncidentWorksitesSyncStats.SyncTimestamps(
            before = fullUpdatedBefore,
            after = fullUpdatedAfter,
        ),
        boundedParameters = boundedParameters,
        boundedSyncTimestamps = IncidentWorksitesSyncStats.SyncTimestamps(
            before = boundedUpdatedBefore,
            after = boundedUpdatedAfter,
        ),
        appBuildVersionCode = appBuildVersionCode,
    )
}
