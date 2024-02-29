package com.crisiscleanup.core.network

import com.crisiscleanup.core.model.data.InitiatePhoneLoginResult
import com.crisiscleanup.core.network.model.InitiatePasswordResetResult
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

interface CrisisCleanupAccountApi {
    suspend fun initiateMagicLink(emailAddress: String): Boolean

    suspend fun initiatePhoneLogin(phoneNumber: String): InitiatePhoneLoginResult

    suspend fun initiatePasswordReset(emailAddress: String): InitiatePasswordResetResult

    suspend fun changePassword(
        password: String,
        token: String,
    ): Boolean

    suspend fun acceptTerms(userId: Long, timestamp: Instant = Clock.System.now()): Boolean

    suspend fun requestRedeploy(organizationId: Long, incidentId: Long): Boolean
}
