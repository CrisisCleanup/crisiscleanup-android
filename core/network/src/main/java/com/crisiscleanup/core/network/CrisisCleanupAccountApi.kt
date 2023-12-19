package com.crisiscleanup.core.network

import com.crisiscleanup.core.network.model.InitiatePasswordResetResult

interface CrisisCleanupAccountApi {
    suspend fun initiateMagicLink(emailAddress: String): Boolean

    suspend fun initiatePhoneLogin(phoneNumber: String): Boolean

    suspend fun initiatePasswordReset(emailAddress: String): InitiatePasswordResetResult

    suspend fun changePassword(
        password: String,
        token: String,
    ): Boolean
}
