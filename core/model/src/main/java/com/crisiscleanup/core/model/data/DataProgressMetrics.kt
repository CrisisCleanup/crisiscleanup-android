package com.crisiscleanup.core.model.data

/**
 * Case data download progress description
 */
data class DataProgressMetrics(
    val isSecondaryData: Boolean = false,
    val showProgress: Boolean = false,
    val progress: Float = 0.0f,
    val isLoadingPrimary: Boolean = showProgress && !isSecondaryData,
)

val zeroDataProgress = DataProgressMetrics()
