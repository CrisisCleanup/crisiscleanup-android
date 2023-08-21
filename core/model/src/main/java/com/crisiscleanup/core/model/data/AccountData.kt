package com.crisiscleanup.core.model.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

data class AccountData(
    val id: Long,
    val tokenExpiry: Instant,
    val fullName: String,
    val emailAddress: String,
    val profilePictureUri: String,
    val org: OrgData,
    val areTokensValid: Boolean,
) {
    val hasAuthenticated: Boolean
        get() = id > 0
    val isAccessTokenExpired: Boolean
        get() = tokenExpiry <= Clock.System.now().minus(1.minutes)
}

val emptyOrgData = OrgData(0, "")
val emptyAccountData =
    AccountData(0, Instant.fromEpochSeconds(0), "", "", "", emptyOrgData, true)

data class OrgData(
    val id: Long,
    val name: String,
)
