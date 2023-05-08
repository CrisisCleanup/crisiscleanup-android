package com.crisiscleanup.core.network

import com.crisiscleanup.core.network.model.NetworkAuthResult
import com.crisiscleanup.core.network.model.NetworkCountResult
import com.crisiscleanup.core.network.model.NetworkIncidentOrganizationsResult
import com.crisiscleanup.core.network.model.NetworkIncidentResult
import com.crisiscleanup.core.network.model.NetworkIncidentsResult
import com.crisiscleanup.core.network.model.NetworkLanguageTranslationResult
import com.crisiscleanup.core.network.model.NetworkLanguagesResult
import com.crisiscleanup.core.network.model.NetworkLocationsResult
import com.crisiscleanup.core.network.model.NetworkWorkTypeRequestResult
import com.crisiscleanup.core.network.model.NetworkWorkTypeStatusResult
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import com.crisiscleanup.core.network.model.NetworkWorksiteLocationSearchResult
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
    ): NetworkIncidentsResult

    suspend fun getIncidentLocations(
        locationIds: List<Long>,
    ): NetworkLocationsResult

    suspend fun getIncidentOrganizations(
        incidentId: Long,
        limit: Int,
        offset: Int,
    ): NetworkIncidentOrganizationsResult

    suspend fun getIncident(
        id: Long,
        fields: List<String>,
    ): NetworkIncidentResult

    suspend fun getWorksites(
        incidentId: Long,
        limit: Int,
        offset: Int,
    ): NetworkWorksitesFullResult

    suspend fun getWorksites(
        worksiteIds: Collection<Long>,
    ): NetworkWorksitesFullResult

    suspend fun getWorksite(id: Long): NetworkWorksiteFull?

    suspend fun getWorksitesCount(
        incidentId: Long,
        updatedAtAfter: Instant? = null,
    ): NetworkCountResult

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

    suspend fun getLocationSearchWorksites(
        incidentId: Long,
        q: String,
        limit: Int = 5,
    ): NetworkWorksiteLocationSearchResult

    suspend fun getSearchWorksites(
        incidentId: Long,
        q: String,
    ): NetworkWorksitesShortResult

    suspend fun getLanguages(): NetworkLanguagesResult

    suspend fun getLanguageTranslations(key: String): NetworkLanguageTranslationResult

    suspend fun getLocalizationCount(after: Instant): NetworkCountResult

    suspend fun getWorkTypeRequests(id: Long): NetworkWorkTypeRequestResult
}