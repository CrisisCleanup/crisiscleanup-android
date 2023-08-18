package com.crisiscleanup.feature.authentication

import kotlinx.datetime.Instant

interface AccessTokenDecoder {
    fun decode(accessToken: String): DecodedAccessToken
}

data class DecodedAccessToken(
    val expiresAt: Instant,
)
