package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

data class NetworkImage(
    val id: Long,
    val createdAt: Instant,
    val title: String,
    val thumbnailUrl: String,
    val imageUrl: String,
    val tag: String,
    val isAfter: Boolean = tag.lowercase() == "after",
)
