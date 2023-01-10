package com.crisiscleanup.core.network

import com.crisiscleanup.core.network.model.NetworkAuthResult

interface CrisisCleanupAuthApi {
    suspend fun login(email: String, password: String): NetworkAuthResult

    suspend fun logout()
}