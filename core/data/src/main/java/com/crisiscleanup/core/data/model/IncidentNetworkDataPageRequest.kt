package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.network.model.NetworkIncidentOrganization
import com.crisiscleanup.core.network.model.NetworkWorksiteShort
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class IncidentWorksitesPageRequest(
    val incidentId: Long,
    val requestTime: Instant,
    val page: Int,
    // Indicates the number of records coming before this data
    val startCount: Int,
    val totalCount: Int,
    val worksites: List<NetworkWorksiteShort>,
)

@Serializable
data class IncidentOrganizationsPageRequest(
    val incidentId: Long,
    val offset: Int,
    val totalCount: Int,
    val organizations: List<NetworkIncidentOrganization>,
)