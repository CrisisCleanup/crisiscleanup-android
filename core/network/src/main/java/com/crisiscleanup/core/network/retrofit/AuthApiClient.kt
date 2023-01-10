package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import com.crisiscleanup.core.network.model.NetworkAuthPayload
import com.crisiscleanup.core.network.model.NetworkAuthResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import javax.inject.Singleton

private interface AuthApi {
    @POST("login")
    suspend fun login(@Body body: NetworkAuthPayload): NetworkAuthResult

    @GET("logout")
    suspend fun logout()
}

@Singleton
class AuthApiClient : CrisisCleanupAuthApi {

    private val networkApi = crisisCleanupApiBuilder.create(AuthApi::class.java)

    override suspend fun login(email: String, password: String): NetworkAuthResult =
        networkApi.login(NetworkAuthPayload(email, password))

    override suspend fun logout() = networkApi.logout()
}