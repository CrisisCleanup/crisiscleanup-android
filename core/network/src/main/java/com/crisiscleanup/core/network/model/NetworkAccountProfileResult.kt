package com.crisiscleanup.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class NetworkAccountProfileResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val files: List<NetworkFile>?,
)
