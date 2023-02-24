package com.crisiscleanup.core.network

import com.crisiscleanup.core.network.model.*
import kotlinx.datetime.Instant

interface CrisisCleanupAuthApi {
    suspend fun login(email: String, password: String): NetworkAuthResult

    suspend fun logout()
}

interface CrisisCleanupNetworkDataSource {
    suspend fun getIncidents(
        fields: List<String>,
        limit: Int = 250,
        ordering: String = "-start_at"
    ): NetworkIncidentsResult

    suspend fun getIncidentLocations(
        locationIds: List<Long>,
    ): NetworkLocationsResult

    suspend fun getWorksites(
        incidentId: Long,
        limit: Int,
        offset: Int,
    ): NetworkWorksitesFullResult

    suspend fun getWorksitesCount(
        incidentId: Long,
        updatedAtAfter: Instant? = null,
    ): NetworkWorksitesCountResult

    suspend fun getWorksitesAll(
        incidentId: Long,
        updatedAtAfter: Instant?,
        updatedAtBefore: Instant? = null,
    ): NetworkWorksitesShortResult

    suspend fun getWorksitesPage(
        incidentId: Long,
        updatedAtAfter: Instant?,
        pageCount: Int,
        pageOffset: Int? = null,
        latitude: Double? = null,
        longitude: Double? = null,
    ): NetworkWorksitesShortResult
}