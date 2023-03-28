package com.crisiscleanup.core.mapmarker.util

import com.crisiscleanup.core.mapmarker.model.DefaultCoordinates
import com.crisiscleanup.core.mapmarker.model.IncidentBounds
import com.crisiscleanup.core.mapmarker.model.LocationBounds
import com.crisiscleanup.core.mapmarker.model.LocationLatLng
import com.crisiscleanup.core.model.data.Location
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import kotlin.math.sqrt

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
        val areas = multiBounds.mapIndexed { i, latLngBounds ->
            if (latLngBounds == null) 0.0
            else SphericalUtil.computeArea(it.multiCoordinates[i])
        }
        LocationBounds(it, multiBounds, areas)
    }
    val incidentLatLngBounds = locations
        .map { locationBounds ->
            locationBounds.multiBounds.filterNotNull()
                .map { listOf(it.southwest, it.northeast) }
                .flatten()
        }
        .flatten()
        .toBounds()

    val maxAreaLocation =
        locations.fold(
            Triple<Double, List<LatLng>?, LatLngBounds?>(0.0, null, null)
        ) { accOuter, locationLatLng ->
            var maxArea = 0.0
            var latLngs: List<LatLng>? = null
            var bounds: LatLngBounds? = null
            locationLatLng.boundAreas.forEachIndexed { i, area ->
                if (maxArea < area) {
                    maxArea = area
                    latLngs = locationLatLng.locationLatLng.multiCoordinates[i]
                    bounds = locationLatLng.multiBounds[i]
                }
            }

            if (accOuter.first > maxArea) accOuter
            else Triple(maxArea, latLngs, bounds)
        }
    // TODO Cache the center location on save to database rather than computing every time
    var incidentCentroid = DefaultCoordinates
    val locationPoly = maxAreaLocation.second
    val centerBounds = maxAreaLocation.third
    if (centerBounds != null && locationPoly?.isNotEmpty() == true) {
        incidentCentroid = centerBounds.center
        if (!PolyUtil.containsLocation(incidentCentroid, locationPoly, true)) {
            var closestPoint = locationPoly[0]
            var closestDistance =
                SphericalUtil.computeDistanceBetween(incidentCentroid, closestPoint)
            for (polyPoint in locationPoly) {
                val distance = SphericalUtil.computeDistanceBetween(incidentCentroid, polyPoint)
                if (distance < closestDistance) {
                    closestDistance = distance
                    closestPoint = polyPoint
                }
            }

            var furthestPoint = closestPoint
            var furthestDistance = closestDistance
            val delta = closestPoint.subtract(incidentCentroid)
            val deltaNorm = delta.normalizeOrSelf()
            if (deltaNorm != delta) {
                for (polyPoint in locationPoly) {
                    val polyDelta = polyPoint.subtract(incidentCentroid)
                    val polyDeltaNorm = polyDelta.normalizeOrSelf()
                    if (polyDelta != polyDeltaNorm &&
                        polyDeltaNorm.latitude * deltaNorm.latitude + polyDeltaNorm.longitude * deltaNorm.longitude > 0.9
                    ) {
                        val distance =
                            SphericalUtil.computeDistanceBetween(incidentCentroid, polyPoint)
                        if (distance > furthestDistance) {
                            furthestDistance = distance
                            furthestPoint = polyPoint
                        }
                    }
                }
                if (furthestDistance > closestDistance) {
                    incidentCentroid = SphericalUtil.interpolate(closestPoint, furthestPoint, 0.5)
                }
            }
        }
    }

    return IncidentBounds(
        locations,
        incidentLatLngBounds,
        incidentCentroid,
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

private fun LatLng.subtract(latLng: LatLng) = LatLng(
    latitude - latLng.latitude,
    longitude - latLng.longitude,
)

private fun LatLng.normalizeOrSelf(): LatLng {
    val sqr = latitude * latitude + longitude * longitude
    if (sqr > 0) {
        val nDist = sqrt(sqr)
        return LatLng(latitude / nDist, longitude / nDist)
    }
    return this
}
