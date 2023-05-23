package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import com.crisiscleanup.core.network.model.NetworkAuthPayload
import com.crisiscleanup.core.network.model.NetworkAuthResult
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

private interface AuthApi {
    @ThrowClientErrorHeader
    @POST("api-token-auth")
    suspend fun login(@Body body: NetworkAuthPayload): NetworkAuthResult

    @TokenAuthenticationHeader
    @POST("logout")
    suspend fun logout()
}

@Singleton
class AuthApiClient @Inject constructor(
    @RetrofitConfiguration(RetrofitConfigurations.CrisisCleanup) retrofit: Retrofit,
) : CrisisCleanupAuthApi {
    private val networkApi = retrofit.create(AuthApi::class.java)

    override suspend fun login(email: String, password: String): NetworkAuthResult =
        networkApi.login(NetworkAuthPayload(email, password))

    override suspend fun logout() {}
}