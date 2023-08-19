package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

data class AppOpenInstant(
    val version: Long = 0,
    val date: Instant = Instant.fromEpochSeconds(0),
)
