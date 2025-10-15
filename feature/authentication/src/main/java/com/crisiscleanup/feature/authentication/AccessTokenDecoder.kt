package com.crisiscleanup.feature.authentication

import kotlin.time.Instant

interface AccessTokenDecoder {
    fun decode(accessToken: String): DecodedAccessToken
}

data class DecodedAccessToken(
    val expiresAt: Instant,
)
