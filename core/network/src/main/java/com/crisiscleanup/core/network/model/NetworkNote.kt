package com.crisiscleanup.core.network.model

import com.crisiscleanup.core.network.model.util.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class NetworkNote(
    // Incoming network ID is always defined
    val id: Long?,
    @Serializable(InstantSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("is_survivor")
    val isSurvivor: Boolean,
    val note: String?,
)

@Serializable
data class NetworkNoteNote(
    val note: String,
    @Serializable(InstantSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant,
)
