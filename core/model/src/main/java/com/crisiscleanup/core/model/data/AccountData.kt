package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

data class AccountData(
    val accessToken: String,
    val tokenExpiry: Instant,
    val displayName: String,
)