package com.crisiscleanup.core.model.data

import kotlin.time.Instant

data class JoinOrgInvite(
    val token: String,
    val orgId: Long,
    val expiresAt: Instant,
    val isExpired: Boolean = expiresAt.isPast,
)

enum class JoinOrgResult {
    Success,

    // Already joined
    Redundant,
    PendingAdditionalAction,
    Unknown,
}
