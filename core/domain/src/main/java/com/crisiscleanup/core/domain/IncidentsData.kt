package com.crisiscleanup.core.domain

import com.crisiscleanup.core.model.data.Incident

sealed interface IncidentsData {
    data object Loading : IncidentsData

    data class Incidents(
        val incidents: List<Incident>,
    ) : IncidentsData

    data object Empty : IncidentsData
}
