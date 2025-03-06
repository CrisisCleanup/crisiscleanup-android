package com.crisiscleanup.core.mapmarker

import androidx.lifecycle.AtomicReference
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocationsRepository
import com.crisiscleanup.core.mapmarker.model.IncidentBounds
import com.crisiscleanup.core.mapmarker.util.toBounds
import com.crisiscleanup.core.mapmarker.util.toLatLng
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.IncidentLocation
import com.crisiscleanup.core.model.data.IncidentLocationBounder
import com.google.android.gms.maps.model.LatLng
import javax.inject.Inject

class CrisisCleanupIncidentLocationBounder @Inject constructor(
    private val incidentsRepository: IncidentsRepository,
    private val locationsRepository: LocationsRepository,
) : IncidentLocationBounder {
    private val bounderIncidentId = AtomicReference(EmptyIncident.id)
    private var incidentBounds: IncidentBounds? = null

    private suspend fun getBounds(incidentId: Long): IncidentBounds? {
        if (incidentId == EmptyIncident.id) {
            return null
        }

        var bounds: IncidentBounds? = null
        synchronized(bounderIncidentId) {
            if (bounderIncidentId.get() == incidentId) {
                bounds = incidentBounds
            }
        }

        if (bounds == null) {
            bounds = incidentsRepository.getIncident(incidentId)?.locations
                ?.map(IncidentLocation::location)
                ?.let { locationIds ->
                    val locations = locationsRepository.getLocations(locationIds.toSet())
                    locations.toLatLng().toBounds()
                }

            synchronized(bounderIncidentId) {
                bounderIncidentId.set(incidentId)
                incidentBounds = bounds
            }
        }

        return bounds
    }

    override suspend fun isInBounds(
        incidentId: Long,
        latitude: Double,
        longitude: Double,
    ) = getBounds(incidentId)?.containsLocation(LatLng(latitude, longitude)) ?: false

    override suspend fun getBoundsCenter(incidentId: Long) =
        getBounds(incidentId)?.bounds?.center?.let {
            return@let Pair(it.latitude, it.latitude)
        }
}
