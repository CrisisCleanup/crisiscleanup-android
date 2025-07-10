package com.crisiscleanup.core.model.data

sealed interface IncidentsData {
    data object Loading : IncidentsData

    data class Incidents(
        val incidents: List<Incident>,
        val selected: Incident,
    ) : IncidentsData {
        val selectedId: Long = selected.id
    }

    data object Empty : IncidentsData
}
