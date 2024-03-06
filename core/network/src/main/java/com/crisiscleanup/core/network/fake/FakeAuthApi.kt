package com.crisiscleanup.core.network.fake

import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import com.crisiscleanup.core.network.model.NetworkAuthResult
import com.crisiscleanup.core.network.model.NetworkAuthUserClaims
import com.crisiscleanup.core.network.model.NetworkCodeAuthResult
import com.crisiscleanup.core.network.model.NetworkOauthResult
import com.crisiscleanup.core.network.model.NetworkPhoneOneTimePasswordResult
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import javax.inject.Inject

class FakeAuthApi @Inject constructor() : CrisisCleanupAuthApi {
    override suspend fun login(email: String, password: String): NetworkAuthResult {
        delay(1000)
        return NetworkAuthResult(
            accessToken = "access-token",
            claims = NetworkAuthUserClaims(
                id = 1,
                email = "demo@crisiscleanup.org",
                firstName = "Demo",
                lastName = "User",
                files = emptyList(),
                hasAcceptedTerms = true,
                acceptedTermsTimestamp = Clock.System.now(),
                approvedIncidents = setOf(1),
            ),
        )
    }

    override suspend fun logout() {
        delay(500)
    }

    override suspend fun oauthLogin(email: String, password: String): NetworkOauthResult {
        delay(1000)
        return NetworkOauthResult(
            refreshToken = "refresh-token",
            accessToken = "access-token",
            expiresIn = 3600,
        )
    }

    override suspend fun magicLinkLogin(token: String) = NetworkCodeAuthResult(
        refreshToken = "refresh",
        accessToken = "access",
        expiresIn = 3600,
    )

    override suspend fun verifyPhoneCode(
        phoneNumber: String,
        code: String,
    ) = NetworkPhoneOneTimePasswordResult()

    override suspend fun oneTimePasswordLogin(
        accountId: Long,
        oneTimePasswordId: Long,
    ) = NetworkCodeAuthResult(
        refreshToken = "refresh",
        accessToken = "access",
        expiresIn = 3600,
    )

    private var refreshTokenCounter = 1
    override suspend fun refreshTokens(refreshToken: String): NetworkOauthResult {
        delay(1000)
        return NetworkOauthResult(
            refreshToken = "refresh-token-${refreshTokenCounter++}",
            accessToken = "access-token",
            expiresIn = 3600,
        )
    }
}
