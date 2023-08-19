package com.crisiscleanup.core.network.endoflife

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class NetworkBuildEndOfLife(
    val expires: Instant,
    val title: String?,
    val message: String,
    val link: String?,
)
