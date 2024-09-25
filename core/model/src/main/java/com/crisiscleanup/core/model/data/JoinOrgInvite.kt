package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

/**
 * Token (and data) for joining an organization or team
 */
data class JoinOrgTeamInvite(
    val token: String,
    /**
     * Org ID or team ID
     */
    val targetId: Long,
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
