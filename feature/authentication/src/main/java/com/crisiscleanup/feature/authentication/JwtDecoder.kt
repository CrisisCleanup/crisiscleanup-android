package com.crisiscleanup.feature.authentication

import com.auth0.android.jwt.JWT
import kotlinx.datetime.Instant
import javax.inject.Inject

class JwtDecoder @Inject constructor() : AccessTokenDecoder {
    override fun decode(accessToken: String): DecodedAccessToken {
        val jwt = JWT(accessToken)
        return DecodedJwt(jwt)
    }
}

class DecodedJwt(jwt: JWT) : DecodedAccessToken {
    override val expiresAt: Instant = Instant.fromEpochMilliseconds(jwt.expiresAt!!.time)
}