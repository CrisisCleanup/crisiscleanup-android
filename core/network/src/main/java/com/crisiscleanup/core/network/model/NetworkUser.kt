package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkUser(
    val id: Long,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    val organization: Long,
    val files: List<NetworkFile>,
)


@Serializable
data class NetworkUserProfile(
    val id: Long,
    val email: String,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    val files: List<NetworkFile>?,
    val organization: NetworkOrganizationShort,
) {
    val profilePicUrl: String?
        get() = files?.profilePictureUrl
}