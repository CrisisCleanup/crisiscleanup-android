package com.crisiscleanup.core.network.model

import com.crisiscleanup.core.network.model.util.InstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkFlagsFormDataResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    override val count: Int? = null,
    val results: List<NetworkFlagsFormData>? = null,
) : WorksiteDataResult<NetworkFlagsFormData> {
    override val data: List<NetworkFlagsFormData>? = results
}

@Serializable
data class NetworkFlagsFormData(
    override val id: Long,
    @SerialName("case_number")
    val caseNumber: String,
    @SerialName("form_data")
    val formData: List<KeyDynamicValuePair>,
    val flags: List<NetworkFlag>,
    val phone1: String?,
    @SerialName("reported_by")
    val reportedBy: Long?,
    @Serializable(InstantSerializer::class)
    @SerialName("updated_at")
    override val updatedAt: Instant,
) : WorksiteDataSubset {
    @Serializable
    data class NetworkFlag(
        @SerialName("is_high_priority")
        val isHighPriority: Boolean?,
        @SerialName("reason_t")
        val reasonT: String?,
    )
}
