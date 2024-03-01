package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.model.data.InitiatePhoneLoginResult
import com.crisiscleanup.core.network.CrisisCleanupAccountApi
import com.crisiscleanup.core.network.model.CrisisCleanupNetworkException
import com.crisiscleanup.core.network.model.InitiatePasswordResetResult
import com.crisiscleanup.core.network.model.NetworkAcceptTermsPayload
import com.crisiscleanup.core.network.model.NetworkAccountProfileResult
import com.crisiscleanup.core.network.model.NetworkEmailPayload
import com.crisiscleanup.core.network.model.NetworkIncidentRedeployRequest
import com.crisiscleanup.core.network.model.NetworkMagicLinkResult
import com.crisiscleanup.core.network.model.NetworkPasswordResetPayload
import com.crisiscleanup.core.network.model.NetworkPasswordResetResult
import com.crisiscleanup.core.network.model.NetworkPhoneCodeResult
import com.crisiscleanup.core.network.model.NetworkPhonePayload
import com.crisiscleanup.core.network.model.NetworkRequestRedeploy
import kotlinx.datetime.Instant
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import javax.inject.Inject

private interface AccountApi {
    @Headers("Cookie: ")
    @POST("magic_link")
    suspend fun initiateMagicLink(
        @Body emailPayload: NetworkEmailPayload,
    ): NetworkMagicLinkResult

    @Headers("Cookie: ")
    @ThrowClientErrorHeader
    @POST("otp")
    suspend fun initiatePhoneLogin(
        @Body phonePayload: NetworkPhonePayload,
    ): NetworkPhoneCodeResult

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

    @TokenAuthenticationHeader
    @ThrowClientErrorHeader
    @PATCH("users/{userId}")
    suspend fun acceptTerms(
        @Path("userId") userId: Long,
        @Body acceptTermsPayload: NetworkAcceptTermsPayload,
    ): NetworkAccountProfileResult

    @TokenAuthenticationHeader
    @POST("incident_requests")
    suspend fun requestRedeploy(
        @Body redeployPayload: NetworkRequestRedeploy,
    ): NetworkIncidentRedeployRequest?
}

class AccountApiClient @Inject constructor(
    @RetrofitConfiguration(RetrofitConfigurations.CrisisCleanup) retrofit: Retrofit,
) : CrisisCleanupAccountApi {
    private val accountApi = retrofit.create(AccountApi::class.java)

    override suspend fun initiateMagicLink(emailAddress: String) = accountApi.initiateMagicLink(
        NetworkEmailPayload(emailAddress),
    ).errors == null

    override suspend fun initiatePhoneLogin(phoneNumber: String): InitiatePhoneLoginResult {
        try {
            val result = accountApi.initiatePhoneLogin(NetworkPhonePayload(phoneNumber))
            if (result.errors?.isNotEmpty() != true) {
                return InitiatePhoneLoginResult.Success
            }
        } catch (e: CrisisCleanupNetworkException) {
            if (e.body.contains("Invalid phone number")) {
                return InitiatePhoneLoginResult.PhoneNotRegistered
            }
        }

        return InitiatePhoneLoginResult.Unknown
    }

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

    override suspend fun acceptTerms(userId: Long, timestamp: Instant): Boolean {
        val payload = NetworkAcceptTermsPayload(true, timestamp)
        val result = accountApi.acceptTerms(userId, payload)
        return result.hasAcceptedTerms == true
    }

    override suspend fun requestRedeploy(organizationId: Long, incidentId: Long) =
        accountApi.requestRedeploy(
            NetworkRequestRedeploy(
                organizationId,
                incidentId,
            ),
        )?.let { it.organization == organizationId && it.incident == incidentId } == true
}
