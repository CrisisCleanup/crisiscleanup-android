package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.database.model.IncidentWorksitesSyncStatsEntity
import com.crisiscleanup.core.model.data.IncidentWorksitesSyncStats
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun IncidentWorksitesSyncStatsEntity.asExternalModel(logger: AppLogger): IncidentWorksitesSyncStats {
    val boundedParameters = try {
        Json.decodeFromString<IncidentWorksitesSyncStats.SyncBoundedParameters>(boundedParameters)
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
        boundedParameters = boundedParameters,
        boundedSyncSteps = IncidentWorksitesSyncStats.SyncStepTimestamps(
            short = IncidentWorksitesSyncStats.SyncTimestamps(
                before = boundedUpdatedBefore,
                after = boundedUpdatedAfter,
            ),
            full = IncidentWorksitesSyncStats.SyncTimestamps(
                before = boundedFullUpdatedBefore,
                after = boundedFullUpdatedAfter,
            ),
        ),
        appBuildVersionCode = appBuildVersionCode,
    )
}

fun IncidentWorksitesSyncStats.asEntity(logger: AppLogger): IncidentWorksitesSyncStatsEntity {
    val boundedParameters = boundedParameters?.let {
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
        boundedParameters = boundedParameters,
        boundedUpdatedBefore = boundedSyncSteps.short.before,
        boundedUpdatedAfter = boundedSyncSteps.short.after,
        boundedFullUpdatedBefore = boundedSyncSteps.full.before,
        boundedFullUpdatedAfter = boundedSyncSteps.full.after,
        appBuildVersionCode = appBuildVersionCode,
    )
}