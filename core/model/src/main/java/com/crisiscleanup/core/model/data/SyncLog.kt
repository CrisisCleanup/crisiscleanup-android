package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

data class SyncLog(
    val logTime: Instant,
    val logType: String,
    val message: String,
    val details: String,
)
