package com.crisiscleanup.core.model.data

import java.net.URL
import kotlin.time.Instant

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
