package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkPortalConfig(
    val attr: NetworkClaimThreshold,
)

@Serializable data class NetworkClaimThreshold(
    @SerialName("claimed_work_type_count_threshold")
    val workTypeCount: Int,
    @SerialName("claimed_work_type_closed_ratio_threshold")
    val workTypeClosedRatio: Float,
)
