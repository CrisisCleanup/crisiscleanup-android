package com.crisiscleanup.feature.authentication

import com.auth0.android.jwt.JWT
import javax.inject.Inject
import kotlin.time.Instant

class JwtDecoder @Inject constructor() : AccessTokenDecoder {
    override fun decode(accessToken: String): DecodedAccessToken {
        val jwt = JWT(accessToken)
        val expiresAt = Instant.fromEpochMilliseconds(jwt.expiresAt!!.time)
        return DecodedAccessToken(expiresAt)
    }
}
