package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import com.crisiscleanup.core.network.model.NetworkAuthPayload
import com.crisiscleanup.core.network.model.NetworkAuthResult
import com.crisiscleanup.core.network.model.NetworkCodeAuthResult
import com.crisiscleanup.core.network.model.NetworkOauthPayload
import com.crisiscleanup.core.network.model.NetworkOauthResult
import com.crisiscleanup.core.network.model.NetworkOneTimePasswordPayload
import com.crisiscleanup.core.network.model.NetworkPhoneCodePayload
import com.crisiscleanup.core.network.model.NetworkPhoneOneTimePasswordResult
import com.crisiscleanup.core.network.model.NetworkRefreshToken
import kotlinx.coroutines.sync.Mutex
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

private interface AuthApi {
    @ThrowClientErrorHeader
    @POST("api-token-auth")
    suspend fun login(@Body body: NetworkAuthPayload): NetworkAuthResult

    @ThrowClientErrorHeader
    @POST("api-mobile-auth")
    suspend fun oauthLogin(@Body body: NetworkOauthPayload): NetworkOauthResult

    @ThrowClientErrorHeader
    @POST("magic_link/{code}/login")
    suspend fun magicLinkCodeAuth(@Path("code") token: String): NetworkCodeAuthResult

    @ThrowClientErrorHeader
    @POST("otp/verify")
    suspend fun verifyPhoneCode(@Body body: NetworkPhoneCodePayload): NetworkPhoneOneTimePasswordResult

    @ThrowClientErrorHeader
    @POST("otp/generate_token")
    suspend fun oneTimePasswordAuth(@Body body: NetworkOneTimePasswordPayload): NetworkCodeAuthResult

    @ThrowClientErrorHeader
    @POST("api-mobile-refresh-token")
    suspend fun refreshAccountTokens(@Body body: NetworkRefreshToken): NetworkOauthResult
}

@Singleton
class AuthApiClient @Inject constructor(
    @RetrofitConfiguration(RetrofitConfigurations.Auth) retrofit: Retrofit,
) : CrisisCleanupAuthApi {
    private val networkApi = retrofit.create(AuthApi::class.java)

    private val refreshMutex = Mutex()

    private fun tryRandomSleep() {
        val sleepMs = 1_000_000L + Random.nextLong(100_000L, 500_000L)
        Thread.sleep(sleepMs)
    }

    override suspend fun login(email: String, password: String): NetworkAuthResult {
        for (i in 0..<3) {
            val loginResult = networkApi.login(NetworkAuthPayload(email, password))
            if (loginResult.claims != null) {
                return loginResult
            }
            tryRandomSleep()
        }
        throw Exception("Failed to login with email and password")
    }

    override suspend fun oauthLogin(email: String, password: String): NetworkOauthResult {
        for (i in 0..<3) {
            val oauthResult = networkApi.oauthLogin(NetworkOauthPayload(email, password))
            if (oauthResult.accessToken.isNotBlank()) {
                return oauthResult
            }
            tryRandomSleep()
        }
        throw Exception("Failed to authenticate email and password")
    }

    override suspend fun magicLinkLogin(token: String) = networkApi.magicLinkCodeAuth(token)

    override suspend fun verifyPhoneCode(
        phoneNumber: String,
        code: String,
    ) = networkApi.verifyPhoneCode(NetworkPhoneCodePayload(phoneNumber, code))

    override suspend fun oneTimePasswordLogin(
        accountId: Long,
        oneTimePasswordId: Long,
    ) = networkApi.oneTimePasswordAuth(NetworkOneTimePasswordPayload(accountId, oneTimePasswordId))

    override suspend fun refreshTokens(refreshToken: String): NetworkOauthResult? {
        if (refreshMutex.tryLock()) {
            try {
                return networkApi.refreshAccountTokens(NetworkRefreshToken(refreshToken))
            } finally {
                refreshMutex.unlock()
            }
        }
        return null
    }

    override suspend fun logout() {}
}
