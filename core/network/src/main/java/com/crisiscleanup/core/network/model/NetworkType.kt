package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class NetworkType(
    val id: Long?,
    @SerialName("type_t")
    val typeT: String,
    @SerialName("created_at")
    val createdAt: Instant?,
)

internal val networkTypeFavorite = NetworkType(null, "favorite", null)
