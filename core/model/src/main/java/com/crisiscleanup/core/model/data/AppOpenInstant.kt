package com.crisiscleanup.core.model.data

import kotlin.time.Instant

data class AppOpenInstant(
    val version: Long = 0,
    val date: Instant = Instant.fromEpochSeconds(0),
)
