package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.network.CrisisCleanupAccountApi
import com.crisiscleanup.core.network.model.InitiatePasswordResetResult
import com.crisiscleanup.core.network.model.NetworkEmailPayload
import com.crisiscleanup.core.network.model.NetworkMagicLinkResult
import com.crisiscleanup.core.network.model.NetworkPasswordResetPayload
import com.crisiscleanup.core.network.model.NetworkPasswordResetResult
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import javax.inject.Inject

private interface AccountApi {
    @POST("magic_link")
    suspend fun initiateMagicLink(
        @Body emailPayload: NetworkEmailPayload,
    ): NetworkMagicLinkResult

    @POST("password_reset_requests")
    suspend fun initiatePasswordReset(
        @Body emailPayload: NetworkEmailPayload,
    ): InitiatePasswordResetResult

    @POST("password_reset_requests/{token}/reset")
    suspend fun resetPassword(
        @Path("token") token: String,
        @Body passwordResetPayload: NetworkPasswordResetPayload,
    ): NetworkPasswordResetResult

    @GET("password_reset_requests/{token}")
    suspend fun resetPasswordStatus(
        @Path("token") token: String,
    ): NetworkPasswordResetResult
}

class AccountApiClient @Inject constructor(
    @RetrofitConfiguration(RetrofitConfigurations.CrisisCleanup) retrofit: Retrofit,
) : CrisisCleanupAccountApi {
    private val accountApi = retrofit.create(AccountApi::class.java)

    override suspend fun initiateMagicLink(emailAddress: String) = accountApi.initiateMagicLink(
        NetworkEmailPayload(emailAddress),
    ).detail.isNotBlank()

    override suspend fun initiatePasswordReset(emailAddress: String) =
        accountApi.initiatePasswordReset(
            NetworkEmailPayload(emailAddress),
        )

    override suspend fun changePassword(password: String, token: String): Boolean {
        val status = accountApi.resetPassword(
            token,
            NetworkPasswordResetPayload(password, token),
        ).status
        return status.isNotBlank() && status != "invalid"
    }
}
