package com.crisiscleanup.core.model.data

import kotlin.time.Instant

data class AppMetricsData(
    @Deprecated("Was for an initial release of app under different account")
    val earlybirdEndOfLife: BuildEndOfLife,

    val appOpen: AppOpenInstant,

    @Deprecated("Was for an early release originally pointed at staging backend needing to point to production on actual release")
    val switchToProductionApiVersion: Long,

    val minSupportedAppVersion: MinSupportedAppVersion,

    val appInstallVersion: Long,
    val appPublishedVersion: Long,
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
