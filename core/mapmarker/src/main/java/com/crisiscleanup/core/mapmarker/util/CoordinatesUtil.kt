package com.crisiscleanup.core.mapmarker.util

import com.crisiscleanup.core.mapmarker.model.DefaultCoordinates
import com.crisiscleanup.core.mapmarker.model.IncidentBounds
import com.crisiscleanup.core.mapmarker.model.LocationBounds
import com.crisiscleanup.core.mapmarker.model.LocationLatLng
import com.crisiscleanup.core.model.data.Location
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

fun Pair<Double, Double>.toLatLng() = LatLng(first, second)

// TODO Invoke map changes more elegantly
fun LatLng.smallOffset() = LatLng(
    latitude,
    longitude + Math.random() * 1e-9,
)

fun Collection<Location>.toLatLng(): Collection<LocationLatLng> {
    return filter {
        !(it.multiCoordinates == null && it.coordinates == null)
    }
        .map {
            val multiCoordinates = it.multiCoordinates ?: listOf(it.coordinates!!)
            val multiLatLngs = multiCoordinates.map { coords ->
                val latLngs = mutableListOf<LatLng>()
                for (i in 1 until coords.size step 2) {
                    latLngs.add(LatLng(coords[i], coords[i - 1]))
                }
                latLngs
            }
            LocationLatLng(
                it.id,
                it.shape,
                multiLatLngs,
            )
        }
}

fun Collection<LocationLatLng>.flattenLatLng() = map { it.multiCoordinates.flatten() }.flatten()

fun Collection<LocationLatLng>.toBounds(): IncidentBounds {
    val locations = map {
        val multiBounds = it.multiCoordinates.map { latLngs ->
            if (latLngs.size < 3) null
            else latLngs.toBounds()
        }
        LocationBounds(it, multiBounds)
    }
    val incidentLatLngBounds = locations
        .map { locationBounds ->
            locationBounds.multiBounds.filterNotNull()
                .map { listOf(it.southwest, it.northeast) }
                .flatten()
        }
        .flatten()
        .toBounds()

    // TODO Use largest shape, by area, and iterate if center is outside a "concave" shape. See SphericalUtil.computeArea
    val maxPointsLocation = locations.fold(
        Pair<List<LatLng>, LocationBounds?>(emptyList(), null)
    ) { accOuter, locationLatLng ->
        val mostPoints =
            locationLatLng.locationLatLng.multiCoordinates.fold(emptyList<LatLng>()) { accInner, latLngs ->
                if (accInner.size > latLngs.size) accInner
                else latLngs
            }

        if (accOuter.first.size > mostPoints.size) accOuter
        else Pair(mostPoints, locationLatLng)
    }
    var incidentCenter = DefaultCoordinates
    maxPointsLocation.second?.let { locationBounds ->
        locationBounds.locationLatLng.multiCoordinates.forEachIndexed { index, latLngs ->
            if (latLngs.size == maxPointsLocation.first.size) {
                locationBounds.multiBounds.get(index)?.let { latLngBounds ->
                    incidentCenter = latLngBounds.center
                }
            }
        }
    }

    return IncidentBounds(
        locations,
        incidentLatLngBounds,
        incidentCenter,
    )
}

fun Collection<LatLng>.toBounds(
    startingBounds: LatLngBounds,
    minLatSpan: Double = 0.0001,
    minLngSpan: Double = 0.0002,
): LatLngBounds {
    val locationBounds = fold(LatLngBounds.builder()) { acc, latLng -> acc.include(latLng) }

    var sw = startingBounds.southwest
    var ne = startingBounds.northeast
    val center = startingBounds.center
    if (ne.latitude - sw.latitude < minLatSpan) {
        val halfSpan = minLatSpan * 0.5
        ne = LatLng(center.latitude + halfSpan, ne.longitude)
        sw = LatLng(center.latitude - halfSpan, sw.longitude)
    }
    // TODO Write tests
    if (sw.longitude + 360 - ne.longitude < minLngSpan) {
        val halfSpan = minLngSpan * 0.5
        var eastLng = center.latitude - halfSpan
        if (eastLng > 180) {
            eastLng -= 360
        }
        var westLng = center.longitude - halfSpan
        if (westLng < -180) {
            westLng += 360
        }
        ne = LatLng(ne.latitude, eastLng)
        sw = LatLng(sw.latitude, westLng)
    }
    locationBounds.include(sw)
    locationBounds.include(ne)

    return locationBounds.build()
}

fun Collection<LatLng>.toBounds(): LatLngBounds {
    val coordinates = if (isEmpty()) DefaultCoordinates else first()
    return toBounds(LatLngBounds(coordinates, coordinates))
}