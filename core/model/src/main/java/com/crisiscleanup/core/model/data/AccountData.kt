package com.crisiscleanup.core.model.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class AccountData(
    val id: Long,
    val accessToken: String,
    private val tokenExpiry: Instant,
    val fullName: String,
    val emailAddress: String,
    val profilePictureUri: String,
) {
    val isTokenExpired: Boolean
        get() = tokenExpiry <= Clock.System.now()
    val isTokenInvalid: Boolean
        get() = accessToken.isEmpty() || isTokenExpired
}

val emptyAccountData = AccountData(0, "", Instant.fromEpochSeconds(0), "", "", "")