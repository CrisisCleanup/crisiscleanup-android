package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import com.crisiscleanup.core.model.data.SyncLog

data class PopulatedSyncLog(
    @Embedded
    val entity: SyncLogEntity,
)

fun PopulatedSyncLog.asExternalModel() = with(entity) {
    SyncLog(
        id = id,
        logTime = logTime,
        logType = logType,
        message = message,
        details = details,
    )
}
