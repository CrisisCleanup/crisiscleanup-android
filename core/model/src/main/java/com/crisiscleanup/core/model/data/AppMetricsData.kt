package com.crisiscleanup.core.model.data

data class AppMetricsData(
    val earlybirdEndOfLife: BuildEndOfLife,

    val appOpen: AppOpenInstant,

    val switchToProductionApiVersion: Long,
)
