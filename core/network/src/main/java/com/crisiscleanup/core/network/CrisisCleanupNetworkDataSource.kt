package com.crisiscleanup.core.network

import com.crisiscleanup.core.network.model.NetworkAuthResult
import com.crisiscleanup.core.network.model.NetworkCaseHistoryEvent
import com.crisiscleanup.core.network.model.NetworkCountResult
import com.crisiscleanup.core.network.model.NetworkIncident
import com.crisiscleanup.core.network.model.NetworkIncidentOrganization
import com.crisiscleanup.core.network.model.NetworkLanguageDescription
import com.crisiscleanup.core.network.model.NetworkLanguageTranslation
import com.crisiscleanup.core.network.model.NetworkLocation
import com.crisiscleanup.core.network.model.NetworkOauthResult
import com.crisiscleanup.core.network.model.NetworkOrganizationsResult
import com.crisiscleanup.core.network.model.NetworkPersonContact
import com.crisiscleanup.core.network.model.NetworkWorkTypeRequest
import com.crisiscleanup.core.network.model.NetworkWorkTypeStatusResult
import com.crisiscleanup.core.network.model.NetworkWorksiteCoreData
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import com.crisiscleanup.core.network.model.NetworkWorksiteLocationSearch
import com.crisiscleanup.core.network.model.NetworkWorksitePage
import com.crisiscleanup.core.network.model.NetworkWorksiteShort
import kotlinx.datetime.Instant

interface CrisisCleanupAuthApi {
    suspend fun login(email: String, password: String): NetworkAuthResult
    suspend fun oauthLogin(email: String, password: String): NetworkOauthResult
    suspend fun refreshTokens(refreshToken: String): NetworkOauthResult?
    suspend fun logout()
}

interface CrisisCleanupNetworkDataSource {
    suspend fun getProfilePic(): String?

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
    ): NetworkOrganizationsResult

    suspend fun getIncident(
        id: Long,
        fields: List<String>,
    ): NetworkIncident?

    suspend fun getWorksitesCoreData(
        incidentId: Long,
        limit: Int,
        offset: Int,
    ): List<NetworkWorksiteCoreData>?

    suspend fun getWorksites(
        worksiteIds: Collection<Long>,
    ): List<NetworkWorksiteFull>?

    suspend fun getWorksite(id: Long): NetworkWorksiteFull?

    suspend fun getWorksitesCount(
        incidentId: Long,
        updatedAtAfter: Instant? = null,
    ): Int

    suspend fun getWorksitesPage(
        incidentId: Long,
        pageCount: Int,
        pageOffset: Int? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        updatedAtAfter: Instant? = null,
    ): List<NetworkWorksitePage>

    suspend fun getLocationSearchWorksites(
        incidentId: Long,
        q: String,
        limit: Int = 5,
    ): List<NetworkWorksiteLocationSearch>

    suspend fun getSearchWorksites(
        incidentId: Long,
        q: String,
        filters: Map<String, Any> = emptyMap(),
    ): List<NetworkWorksiteShort>

    suspend fun getLanguages(): List<NetworkLanguageDescription>

    suspend fun getLanguageTranslations(key: String): NetworkLanguageTranslation?

    suspend fun getLocalizationCount(after: Instant): NetworkCountResult

    suspend fun getWorkTypeRequests(id: Long): List<NetworkWorkTypeRequest>

    suspend fun getNearbyOrganizations(
        latitude: Double,
        longitude: Double,
    ): List<NetworkIncidentOrganization>

    suspend fun searchUsers(
        q: String,
        organization: Long,
        limit: Int = 10,
    ): List<NetworkPersonContact>

    suspend fun getCaseHistory(worksiteId: Long): List<NetworkCaseHistoryEvent>
}