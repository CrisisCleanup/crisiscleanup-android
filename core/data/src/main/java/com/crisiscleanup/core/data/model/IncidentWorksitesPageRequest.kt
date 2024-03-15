package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.network.model.NetworkFlagsFormData
import com.crisiscleanup.core.network.model.NetworkIncidentOrganization
import com.crisiscleanup.core.network.model.NetworkWorksitePage
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

interface IncidentCacheDataPageRequest {
    val incidentId: Long
    val requestTime: Instant
    val page: Int
    val startCount: Int
    val totalCount: Int
}

@Serializable
data class IncidentWorksitesPageRequest(
    override val incidentId: Long,
    override val requestTime: Instant,
    override val page: Int,
    // Indicates the number of records coming before this data
    override val startCount: Int,
    override val totalCount: Int,
    val worksites: List<NetworkWorksitePage>,
) : IncidentCacheDataPageRequest

@Serializable
data class IncidentOrganizationsPageRequest(
    val incidentId: Long,
    val offset: Int,
    val totalCount: Int,
    val organizations: List<NetworkIncidentOrganization>,
)

@Serializable
data class IncidentWorksitesSecondaryDataPageRequest(
    override val incidentId: Long,
    override val requestTime: Instant,
    override val page: Int,
    // Indicates the number of records coming before this data
    override val startCount: Int,
    override val totalCount: Int,
    val secondaryData: List<NetworkFlagsFormData>,
) : IncidentCacheDataPageRequest
