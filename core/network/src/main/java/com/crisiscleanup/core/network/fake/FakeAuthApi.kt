package com.crisiscleanup.core.network.fake

import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import com.crisiscleanup.core.network.model.NetworkAuthResult
import com.crisiscleanup.core.network.model.NetworkAuthUserClaims
import com.crisiscleanup.core.network.model.NetworkOauthResult
import kotlinx.coroutines.delay
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
        )
    }

    private var refreshTokenCounter = 1
    override suspend fun refreshTokens(refreshToken: String): NetworkOauthResult {
        delay(1000)
        return NetworkOauthResult(
            refreshToken = "refresh-token-${refreshTokenCounter++}",
            accessToken = "access-token",
        )
    }
}