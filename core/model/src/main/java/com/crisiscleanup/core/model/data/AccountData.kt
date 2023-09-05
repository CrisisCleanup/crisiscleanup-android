package com.crisiscleanup.core.model.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

/**
 * General data on a user account
 *
 * Tokens are stored elsewhere. Data here does not guarantee anything about auth tokens. Token related data here can mostly be trusted.
 */
data class AccountData(
    /**
     * Crisis Cleanup user ID
     *
     * A value of 0 is a user who has never authenticated successfully.
     * A positive value means the user has authenticated at least once.
     * A positive value does not indicate anything about the state of auth tokens.
     */
    val id: Long,

    /**
     * Initial expiry time of the access token
     *
     * This indicates the original expiry of the access token and makes no determination about the current state of the access token.
     * The access token could have been invalidated.
     */
    val tokenExpiry: Instant,

    val fullName: String,
    val emailAddress: String,
    val profilePictureUri: String,
    val org: OrgData,

    /**
     * Indicates the refresh token was still valid when last used
     *
     * When FALSE the user should be notified to re-authenticate as the refresh token is sure to be invalid on next use (if not already cleared).
     */
    val areTokensValid: Boolean,
) {
    /**
     * Indicates if the user has logged in at least once.
     */
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
