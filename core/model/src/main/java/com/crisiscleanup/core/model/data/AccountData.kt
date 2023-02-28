package com.crisiscleanup.core.model.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class AccountData(
    val id: Long,
    val accessToken: String,
    val tokenExpiry: Instant,
    val fullName: String,
    val emailAddress: String,
    val profilePictureUri: String,
    val org: OrgData,
) {
    val isTokenExpired: Boolean
        get() = tokenExpiry <= Clock.System.now()
    val isTokenInvalid: Boolean
        get() = accessToken.isEmpty() || isTokenExpired
}

val emptyOrgData = OrgData(0, "")
val emptyAccountData = AccountData(0, "", Instant.fromEpochSeconds(0), "", "", "", emptyOrgData)

data class OrgData(
    val id: Long,
    val name: String,
)