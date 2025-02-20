package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.network.model.NetworkIncidentOrganization
import kotlinx.serialization.Serializable

@Serializable
data class IncidentOrganizationsPageRequest(
    val incidentId: Long,
    val offset: Int,
    val totalCount: Int,
    val organizations: List<NetworkIncidentOrganization>,
)
