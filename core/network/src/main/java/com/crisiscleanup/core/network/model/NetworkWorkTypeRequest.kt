package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class NetworkWorkTypeRequestResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkWorkTypeRequest>? = null,
)

@Serializable
data class NetworkWorkTypeRequest(
    val id: Long,
    @SerialName("worksite_work_type")
    val workType: NetworkWorkType,
    @SerialName("requested_by")
    val requestedBy: Long,
    @SerialName("approved_at")
    val approvedAt: Instant?,
    @SerialName("rejected_at")
    val rejectedAt: Instant?,
    @SerialName("token_expiration")
    val tokenExpiration: Instant,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("accepted_rejected_reason")
    val acceptedRejectedReason: String?,
    @SerialName("requested_by_org")
    val byOrg: NetworkOrganizationShort,
    @SerialName("requested_to_org")
    val toOrg: NetworkOrganizationShort,
    val worksite: Long,
)
