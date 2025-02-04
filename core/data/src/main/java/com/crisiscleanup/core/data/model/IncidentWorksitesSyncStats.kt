package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.database.model.IncidentWorksitesSyncStatsEntity
import com.crisiscleanup.core.model.data.IncidentWorksitesSyncStats
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun IncidentWorksitesSyncStatsEntity.asExternalModel(logger: AppLogger): IncidentWorksitesSyncStats {
    val boundedRegion = try {
        Json.decodeFromString<IncidentWorksitesSyncStats.BoundedRegion>(boundedRegion)
    } catch (e: Exception) {
        logger.logException(e)
        null
    }
    return IncidentWorksitesSyncStats(
        incidentId = id,
        syncSteps = IncidentWorksitesSyncStats.SyncStepTimestamps(
            short = IncidentWorksitesSyncStats.SyncTimestamps(
                before = updatedBefore,
                after = updatedAfter,
            ),
            full = IncidentWorksitesSyncStats.SyncTimestamps(
                before = fullUpdatedBefore,
                after = fullUpdatedAfter,
            ),
        ),
        boundedRegion = boundedRegion,
        boundedSyncedAt = boundedSyncedAt,
        appBuildVersionCode = appBuildVersionCode,
    )
}

fun IncidentWorksitesSyncStats.asEntity(logger: AppLogger): IncidentWorksitesSyncStatsEntity {
    val boundedRegion = boundedRegion?.let {
        try {
            Json.encodeToString(it)
        } catch (e: Exception) {
            logger.logException(e)
            ""
        }
    } ?: ""
    return IncidentWorksitesSyncStatsEntity(
        incidentId,
        updatedBefore = syncSteps.short.before,
        updatedAfter = syncSteps.short.after,
        fullUpdatedBefore = syncSteps.full.before,
        fullUpdatedAfter = syncSteps.full.after,
        boundedRegion = boundedRegion,
        boundedSyncedAt = boundedSyncedAt,
        appBuildVersionCode = appBuildVersionCode,
    )
}
