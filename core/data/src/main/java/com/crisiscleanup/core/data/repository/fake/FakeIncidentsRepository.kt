package com.crisiscleanup.core.data.repository.fake

import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.model.data.Incident
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class FakeIncidentsRepository @Inject constructor() : IncidentsRepository {
    override val isLoading: Flow<Boolean> = flowOf(false)

    override val incidents: Flow<List<Incident>> = flowOf(
        listOf(
            makeIncident(1, "Swirling Hurricanes"),
            makeIncident(2, "Mighty Cyclones"),
            makeIncident(3, "Rockin Quakes"),
            makeIncident(4, "Wavy Tsunamies"),
            makeIncident(5, "Drudging Landslides"),
            makeIncident(6, "Meteoric Dustbowl"),
            makeIncident(7, "Shivering Blizzard"),
            makeIncident(8, "Heavy Hail"),
            makeIncident(9, "Torrential Rain"),
            makeIncident(11, "2 Swirling Hurricanes"),
            makeIncident(12, "2 Mighty Cyclones"),
            makeIncident(13, "2 Rockin Quakes"),
            makeIncident(14, "2 Wavy Tsunamies"),
            makeIncident(15, "2 Drudging Landslides"),
            makeIncident(16, "2 Meteoric Dustbowl"),
            makeIncident(17, "2 Shivering Blizzard"),
            makeIncident(18, "2 Heavy Hail"),
            makeIncident(19, "2 Torrential Rain"),
        )
    )

    override suspend fun getIncident(id: Long, loadFormFields: Boolean): Incident? {
        return makeIncident(id, "Incident $id")
    }

    override suspend fun pullIncidents() {}

    override suspend fun pullIncident(id: Long) {}
}

private fun makeIncident(id: Long, name: String) =
    Incident(id, name, name, emptyList(), emptyList(), emptyList())