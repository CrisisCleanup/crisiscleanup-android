package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import com.crisiscleanup.core.network.model.NetworkAuthPayload
import com.crisiscleanup.core.network.model.NetworkAuthResult
import com.crisiscleanup.core.network.model.NetworkOauthPayload
import com.crisiscleanup.core.network.model.NetworkOauthResult
import com.crisiscleanup.core.network.model.NetworkRefreshToken
import kotlinx.coroutines.sync.Mutex
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
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
    @POST("api-mobile-refresh-token")
    suspend fun refreshAccountTokens(@Body body: NetworkRefreshToken): NetworkOauthResult
}

@Singleton
class AuthApiClient @Inject constructor(
    @RetrofitConfiguration(RetrofitConfigurations.Auth) retrofit: Retrofit,
) : CrisisCleanupAuthApi {
    private val networkApi = retrofit.create(AuthApi::class.java)

    private val refreshMutex = Mutex()

    override suspend fun login(email: String, password: String): NetworkAuthResult =
        networkApi.login(NetworkAuthPayload(email, password))

    override suspend fun oauthLogin(email: String, password: String): NetworkOauthResult =
        networkApi.oauthLogin(NetworkOauthPayload(email, password))

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
