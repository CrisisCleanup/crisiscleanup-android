package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.database.model.IncidentDataSyncParametersEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun IncidentDataSyncParametersEntity.asExternalModel(logger: AppLogger): IncidentDataSyncParameters {
    val boundedRegion = if (boundedRegion.isNotBlank()) {
        try {
            Json.decodeFromString<IncidentDataSyncParameters.BoundedRegion>(boundedRegion)
        } catch (e: Exception) {
            logger.logException(e)
            null
        }
    } else {
        null
    }
    return IncidentDataSyncParameters(
        incidentId = id,
        syncDataMeasures = IncidentDataSyncParameters.SyncDataMeasure(
            short = IncidentDataSyncParameters.SyncTimeMarker(
                before = updatedBefore,
                after = updatedAfter,
            ),
            full = IncidentDataSyncParameters.SyncTimeMarker(
                before = fullUpdatedBefore,
                after = fullUpdatedAfter,
            ),
        ),
        boundedRegion = boundedRegion,
        boundedSyncedAt = boundedSyncedAt,
    )
}

fun IncidentDataSyncParameters.asEntity(logger: AppLogger): IncidentDataSyncParametersEntity {
    val boundedRegion = boundedRegion?.let {
        try {
            Json.encodeToString(it)
        } catch (e: Exception) {
            logger.logException(e)
            ""
        }
    } ?: ""
    return IncidentDataSyncParametersEntity(
        incidentId,
        updatedBefore = syncDataMeasures.short.before,
        updatedAfter = syncDataMeasures.short.after,
        fullUpdatedBefore = syncDataMeasures.full.before,
        fullUpdatedAfter = syncDataMeasures.full.after,
        boundedRegion = boundedRegion,
        boundedSyncedAt = boundedSyncedAt,
    )
}
