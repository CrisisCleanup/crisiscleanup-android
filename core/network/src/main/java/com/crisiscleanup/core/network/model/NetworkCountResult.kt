package com.crisiscleanup.core.network.model

import kotlinx.serialization.Serializable

// TODO Test
@Serializable
data class NetworkCountResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
)
