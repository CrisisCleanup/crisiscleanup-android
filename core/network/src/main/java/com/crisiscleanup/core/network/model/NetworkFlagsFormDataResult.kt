package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkFlagsFormDataResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkFlagsFormData>? = null,
)

@Serializable
data class NetworkFlagsFormData(
    val id: Long,
    @SerialName("case_number")
    val caseNumber: String,
    @SerialName("form_data")
    val formData: List<KeyDynamicValuePair>,
    val flags: List<NetworkFlag>,
) {
    @Serializable
    data class NetworkFlag(
        @SerialName("is_high_priority")
        val isHighPriority: Boolean?,
        @SerialName("reason_t")
        val reasonT: String?,
    )
}
