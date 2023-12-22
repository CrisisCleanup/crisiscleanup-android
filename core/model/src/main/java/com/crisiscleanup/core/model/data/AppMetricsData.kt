package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

data class AppMetricsData(
    val earlybirdEndOfLife: BuildEndOfLife,

    val appOpen: AppOpenInstant,

    val switchToProductionApiVersion: Long,

    val minSupportedAppVersion: MinSupportedAppVersion,
)

data class MinSupportedAppVersion(
    val minBuild: Long,
    val title: String = "",
    val message: String = "",
    val link: String = "",
    val isUnsupported: Boolean = false,
)

data class BuildEndOfLife(
    val endDate: Instant = Instant.fromEpochSeconds(0),
    val title: String = "",
    val message: String = "",
    val link: String = "",
) {
    val isEndOfLife = endDate.isPast && message.isNotBlank()
}

val EarlybirdEndOfLifeFallback = BuildEndOfLife(
    Instant.fromEpochSeconds(1694982754),
    "This app has expired",
    listOf(
        "A new app should be available.",
        "Search for Crisis Cleanup on Google Play.",
        "Or reach out to Crisis Cleanup team for the replacement.",
        "",
        "Happy volunteering! \uD83D\uDE4C",
    ).joinToString("\n"),
    "https://play.google.com/store/apps/details?id=com.crisiscleanup.prod",
)
