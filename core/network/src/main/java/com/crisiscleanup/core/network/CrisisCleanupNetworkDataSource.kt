package com.crisiscleanup.core.network

import com.crisiscleanup.core.network.model.NetworkAuthResult
import com.crisiscleanup.core.network.model.NetworkCountResult
import com.crisiscleanup.core.network.model.NetworkIncident
import com.crisiscleanup.core.network.model.NetworkIncidentOrganizationsResult
import com.crisiscleanup.core.network.model.NetworkLanguageDescription
import com.crisiscleanup.core.network.model.NetworkLanguageTranslation
import com.crisiscleanup.core.network.model.NetworkLocation
import com.crisiscleanup.core.network.model.NetworkWorkTypeRequest
import com.crisiscleanup.core.network.model.NetworkWorkTypeStatusResult
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import com.crisiscleanup.core.network.model.NetworkWorksiteLocationSearch
import com.crisiscleanup.core.network.model.NetworkWorksiteShort
import com.crisiscleanup.core.network.model.NetworkWorksitesFullResult
import com.crisiscleanup.core.network.model.NetworkWorksitesShortResult
import kotlinx.datetime.Instant

interface CrisisCleanupAuthApi {
    suspend fun login(email: String, password: String): NetworkAuthResult

    suspend fun logout()
}

interface CrisisCleanupNetworkDataSource {
    suspend fun getStatuses(): NetworkWorkTypeStatusResult

    suspend fun getIncidents(
        fields: List<String>,
        limit: Int = 250,
        ordering: String = "-start_at",
        after: Instant? = null,
    ): List<NetworkIncident>

    suspend fun getIncidentLocations(
        locationIds: List<Long>,
    ): List<NetworkLocation>

    suspend fun getIncidentOrganizations(
        incidentId: Long,
        limit: Int,
        offset: Int,
    ): NetworkIncidentOrganizationsResult

    suspend fun getIncident(
        id: Long,
        fields: List<String>,
    ): NetworkIncident?

    suspend fun getWorksites(
        incidentId: Long,
        limit: Int,
        offset: Int,
    ): NetworkWorksitesFullResult

    suspend fun getWorksites(
        worksiteIds: Collection<Long>,
    ): NetworkWorksitesFullResult

    suspend fun getWorksite(id: Long): NetworkWorksiteFull?
    suspend fun getWorksiteShort(id: Long): NetworkWorksiteShort?

    suspend fun getWorksitesCount(
        incidentId: Long,
        updatedAtAfter: Instant? = null,
    ): Int

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
    ): List<NetworkWorksiteShort>

    suspend fun getLocationSearchWorksites(
        incidentId: Long,
        q: String,
        limit: Int = 5,
    ): List<NetworkWorksiteLocationSearch>

    suspend fun getSearchWorksites(
        incidentId: Long,
        q: String,
    ): List<NetworkWorksiteShort>

    suspend fun getLanguages(): List<NetworkLanguageDescription>

    suspend fun getLanguageTranslations(key: String): NetworkLanguageTranslation?

    suspend fun getLocalizationCount(after: Instant): NetworkCountResult

    suspend fun getWorkTypeRequests(id: Long): List<NetworkWorkTypeRequest>
}