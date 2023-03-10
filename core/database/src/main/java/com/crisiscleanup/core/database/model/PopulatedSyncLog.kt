package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import com.crisiscleanup.core.model.data.SyncLog

data class PopulatedSyncLog(
    @Embedded
    val entity: SyncLogEntity,
)

fun PopulatedSyncLog.asExternalModel() = SyncLog(
    logTime = entity.logTime,
    logType = entity.logType,
    message = entity.message,
    details = entity.details,
)
