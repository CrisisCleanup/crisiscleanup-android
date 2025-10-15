package com.crisiscleanup.core.model.data

import kotlin.time.Instant

data class SyncLog(
    val id: Long,
    val logTime: Instant,
    val logType: String,
    val message: String,
    val details: String,
)
