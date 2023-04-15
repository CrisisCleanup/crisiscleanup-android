package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkFavoriteId(
    @SerialName("favorite_id")
    val favoriteId: Long,
)
