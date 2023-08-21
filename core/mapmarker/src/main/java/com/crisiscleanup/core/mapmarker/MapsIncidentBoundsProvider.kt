package com.crisiscleanup.core.mapmarker

import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocationsRepository
import com.crisiscleanup.core.mapmarker.model.DefaultIncidentBounds
import com.crisiscleanup.core.mapmarker.model.IncidentBounds
import com.crisiscleanup.core.mapmarker.util.toBounds
import com.crisiscleanup.core.mapmarker.util.toLatLng
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.IncidentLocation
import com.crisiscleanup.core.model.data.Location
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

interface IncidentBoundsProvider {
    val mappingBoundsIncidentIds: Flow<Set<Long>>
    suspend fun mapIncidentBounds(incident: Incident): Flow<IncidentBounds>
    fun getIncidentBounds(incidentId: Long): IncidentBounds?

    suspend fun isInRecentIncidentBounds(coordinates: LatLng): Incident?
}

@Singleton
class MapsIncidentBoundsProvider @Inject constructor(
    private val incidentsRepository: IncidentsRepository,
    private val locationsRepository: LocationsRepository,
) : IncidentBoundsProvider {
    private val staleDuration = 3.hours
    private val cache = mutableMapOf<Long, CacheEntry>()

    override val mappingBoundsIncidentIds = MutableSharedFlow<Set<Long>>(1)

    init {
        mappingBoundsIncidentIds.tryEmit(emptySet())
    }

    override fun getIncidentBounds(incidentId: Long) = cache[incidentId]?.bounds

    private suspend fun publishIds(id: Long, add: Boolean) {
        val copyIds = mappingBoundsIncidentIds.first().toMutableSet()
        if (add) {
            copyIds.add(id)
        } else {
            copyIds.remove(id)
        }
        mappingBoundsIncidentIds.emit(copyIds)
    }

    private suspend fun cacheIncidentBounds(
        incidentId: Long,
        locations: List<Location>,
        locationIds: Set<Long>,
    ): IncidentBounds {
        publishIds(incidentId, true)
        try {
            val incidentBounds = locations.toLatLng().toBounds()
            cache[incidentId] = CacheEntry(
                bounds = incidentBounds,
                locationIds = locationIds,
                timestamp = Clock.System.now(),
            )

            return incidentBounds
        } finally {
            publishIds(incidentId, false)
        }
    }

    override suspend fun mapIncidentBounds(
        incident: Incident,
    ) = coroutineScope {
        val incidentId = incident.id
        val incidentLocations = incident.locations
        if (incidentId == EmptyIncident.id || incidentLocations.isEmpty()) {
            flowOf(DefaultIncidentBounds)
        } else {
            val locationIds = incidentLocations.map(IncidentLocation::location).toSet()
            locationsRepository.streamLocations(locationIds).mapLatest { locations ->
                cacheIncidentBounds(incidentId, locations, locationIds)
            }
        }
    }

    override suspend fun isInRecentIncidentBounds(coordinates: LatLng): Incident? {
        val startAt = Clock.System.now() - 60.days
        val recentIncidents = incidentsRepository.getIncidents(startAt)
        for (incident in recentIncidents) {
            if (!cache.contains(incident.id)) {
                val locationIds = incident.locations.map(IncidentLocation::location).toSet()
                val locations = locationsRepository.getLocations(locationIds)
                cacheIncidentBounds(incident.id, locations, locationIds)
            }
            cache[incident.id]?.let { cached ->
                if (cached.bounds.containsLocation(coordinates)) {
                    return incident
                }
            }
        }
        return null
    }
}

private data class CacheEntry(
    val bounds: IncidentBounds,
    val locationIds: Set<Long>,
    val timestamp: Instant,
)
