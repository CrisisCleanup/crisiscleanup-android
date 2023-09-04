package com.crisiscleanup.core.network.appsupport

import kotlinx.serialization.Serializable

@Serializable
data class NetworkAppSupportInfo(
    val minBuildVersion: Long,
    val title: String?,
    val message: String,
    val link: String?,
)
