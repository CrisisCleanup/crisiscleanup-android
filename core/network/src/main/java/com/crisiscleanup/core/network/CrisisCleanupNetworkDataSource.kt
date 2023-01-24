package com.crisiscleanup.core.network

import com.crisiscleanup.core.network.model.NetworkAuthResult
import com.crisiscleanup.core.network.model.NetworkIncidentsResult

interface CrisisCleanupAuthApi {
    suspend fun login(email: String, password: String): NetworkAuthResult

    suspend fun logout()
}

interface CrisisCleanupNetworkDataSource {
    suspend fun getIncidents(
        fields: List<String>,
        limit: Int = 100,
        ordering: String = "-start_at"
    ): NetworkIncidentsResult
}