package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant
import java.net.URL

data class OrgUserInviteInfo(
    val displayName: String,
    val inviterEmail: String,
    val inviterAvatarUrl: URL?,
    val invitedEmail: String,
    val orgName: String,
    val expiration: Instant,
    val isExpiredInvite: Boolean,
    val isExistingUser: Boolean,
    val fromOrgName: String = "",
)

val ExpiredNetworkOrgInvite = OrgUserInviteInfo(
    displayName = "",
    inviterEmail = "",
    inviterAvatarUrl = null,
    invitedEmail = "",
    orgName = "",
    expiration = Instant.fromEpochSeconds(0),
    isExpiredInvite = true,
    isExistingUser = false,
)

data class TeamInviteInfo(
    val teamId: Long,
    val expiration: Instant,
    val isExpiredInvite: Boolean,
)

val ExpiredNetworkTeamInvite = TeamInviteInfo(
    teamId = 0,
    expiration = Instant.fromEpochSeconds(0),
    isExpiredInvite = true,
)
