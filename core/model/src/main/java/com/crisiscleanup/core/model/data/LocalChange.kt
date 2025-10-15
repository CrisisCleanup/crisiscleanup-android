package com.crisiscleanup.core.model.data

import kotlin.time.Instant

data class LocalChange(
    val isLocalModified: Boolean,
    val localModifiedAt: Instant,
    val syncedAt: Instant,
)
