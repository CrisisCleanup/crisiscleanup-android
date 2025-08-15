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
