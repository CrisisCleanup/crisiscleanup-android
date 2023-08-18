package com.crisiscleanup.core.model.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class BuildEndOfLife(
    val endDate: Instant = Instant.fromEpochSeconds(0),
    val title: String = "",
    val message: String = "",
    val link: String = "",
) {
    val isEndOfLife = endDate < Clock.System.now() && message.isNotBlank()
}

val EarlybirdEndOfLifeFallback = BuildEndOfLife(
    Instant.fromEpochSeconds(1694982754),
    "This app has wound down",
    "This app has expired. Reach out to developers for the replacement.",
    "https://play.google.com/store/apps/details?id=com.crisiscleanup.prod",
)
