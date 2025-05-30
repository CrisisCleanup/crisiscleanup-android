package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.database.model.IncidentDataSyncParametersEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun IncidentDataSyncParametersEntity.asExternalModel(logger: AppLogger): IncidentDataSyncParameters {
    val savedRegion = if (boundedRegion.isNotBlank()) {
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
            core = IncidentDataSyncParameters.SyncTimeMarker(
                before = updatedBefore,
                after = updatedAfter,
            ),
            additional = IncidentDataSyncParameters.SyncTimeMarker(
                before = additionalUpdatedBefore,
                after = additionalUpdatedAfter,
            ),
        ),
        boundedRegion = savedRegion,
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
        updatedBefore = syncDataMeasures.core.before,
        updatedAfter = syncDataMeasures.core.after,
        additionalUpdatedBefore = syncDataMeasures.additional.before,
        additionalUpdatedAfter = syncDataMeasures.additional.after,
        boundedRegion = boundedRegion,
        boundedSyncedAt = boundedSyncedAt,
    )
}
