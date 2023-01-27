package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

data class AccountData(
    val id: Long,
    val accessToken: String,
    val tokenExpiry: Instant,
    val fullName: String,
    val emailAddress: String,
    val profilePictureUri: String,
)

val emptyAccountData = AccountData(0, "", Instant.fromEpochSeconds(0), "", "", "")