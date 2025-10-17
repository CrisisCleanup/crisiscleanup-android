package com.crisiscleanup.core.network.endoflife

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class NetworkBuildEndOfLife(
    val expires: Instant,
    val title: String?,
    val message: String,
    val link: String?,
)
