package com.crisiscleanup.feature.authentication

import kotlinx.datetime.Instant

interface AccessTokenDecoder {
    fun decode(accessToken: String): DecodedAccessToken
}

interface DecodedAccessToken {
    val expiresAt: Instant
}