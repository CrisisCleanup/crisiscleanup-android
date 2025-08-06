package com.crisiscleanup.core.network

import com.crisiscleanup.core.network.model.NetworkCodeAuthResult
import com.crisiscleanup.core.network.model.NetworkOauthResult
import com.crisiscleanup.core.network.model.NetworkPhoneOneTimePasswordResult

interface CrisisCleanupAuthApi {
    suspend fun oauthLogin(email: String, password: String): NetworkOauthResult

    suspend fun magicLinkLogin(token: String): NetworkCodeAuthResult

    suspend fun verifyPhoneCode(
        phoneNumber: String,
        code: String,
    ): NetworkPhoneOneTimePasswordResult?

    suspend fun oneTimePasswordLogin(
        accountId: Long,
        oneTimePasswordId: Long,
    ): NetworkCodeAuthResult?

    suspend fun refreshTokens(refreshToken: String): NetworkOauthResult?

    suspend fun logout()
}
