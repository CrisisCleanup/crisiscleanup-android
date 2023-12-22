package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

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
