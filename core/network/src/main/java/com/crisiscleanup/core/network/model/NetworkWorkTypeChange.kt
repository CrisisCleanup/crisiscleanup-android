package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class NetworkWorkTypeChangeRequest(
    @SerialName("work_types")
    val workTypes: List<String>,
    @SerialName("requested_reason")
    val reason: String,
)

@Serializable
data class NetworkWorkTypeChangeRelease(
    @SerialName("work_types")
    val workTypes: List<String>,
    @SerialName("unclaim_reason")
    val reason: String,
)