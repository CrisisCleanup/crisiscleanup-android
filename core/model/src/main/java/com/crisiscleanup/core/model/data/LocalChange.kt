package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

data class LocalChange(
    val isLocalModified: Boolean,
    val localModifiedAt: Instant,
    val syncedAt: Instant,
)
