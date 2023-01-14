package com.crisiscleanup.core.network.fake

import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import com.crisiscleanup.core.network.model.NetworkAuthResult
import com.crisiscleanup.core.network.model.NetworkAuthUserClaims
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
}