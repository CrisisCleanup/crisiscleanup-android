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

    override suspend fun login(email: String, password: String) =
        networkApi.login(NetworkAuthPayload(email, password))

    override suspend fun oauthLogin(email: String, password: String) =
        networkApi.oauthLogin(NetworkOauthPayload(email, password))

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
