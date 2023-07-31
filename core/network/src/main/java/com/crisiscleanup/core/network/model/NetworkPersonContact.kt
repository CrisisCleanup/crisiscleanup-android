package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkUsersResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkPersonContact>? = null,
)

@Serializable
data class NetworkPersonContact(
    val id: Long,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    val email: String,
    val mobile: String,
    val organization: ContactOrganization,
) {
    @Serializable
    data class ContactOrganization(
        val id: Long,
        val name: String,
        val affiliates: Collection<Long>,
        @SerialName("type_t")
        val typeT: String?,
    )
}
