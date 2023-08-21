package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.SyncLogEntity
import com.crisiscleanup.core.model.data.SyncLog

fun SyncLog.asEntity() = SyncLogEntity(
    id = 0,
    logTime = logTime,
    logType = logType,
    message = message,
    details = details,
)
