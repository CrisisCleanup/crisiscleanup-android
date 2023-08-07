package com.crisiscleanup.feature.cases.map

import com.crisiscleanup.core.common.AppMemoryStats
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.radians
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.crisiscleanup.feature.cases.map.CoordinateUtil.getMiddleCoordinate
import com.crisiscleanup.feature.cases.map.CoordinateUtil.lerpLatitude
import com.crisiscleanup.feature.cases.map.CoordinateUtil.lerpLongitude
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

internal class CasesMapMarkerManager(
    private val worksitesRepository: WorksitesRepository,
    private val appMemoryStats: AppMemoryStats,
    private val logger: AppLogger,
) {
    private fun getWorksitesCount(id: Long, sw: LatLng, ne: LatLng) =
        worksitesRepository.getWorksitesCount(
            id,
            sw.latitude,
            ne.latitude,
            sw.longitude,
            ne.longitude,
        )

    private fun getBoundQueryParams(
        maxMarkersOnMap: Int,
        incidentId: Long,
        boundsSw: LatLng,
        boundsNe: LatLng,
        middle: LatLng,
    ): BoundsQueryParams {
        var sw = boundsSw
        var ne = boundsNe
        val fullCount = getWorksitesCount(incidentId, sw, ne)
        var queryCount = fullCount
        if (fullCount > maxMarkersOnMap) {
            val halfSw = getMiddleCoordinate(sw, middle)
            val halfNe = getMiddleCoordinate(middle, ne)
            val halfCount = getWorksitesCount(incidentId, halfSw, halfNe)
            if (maxMarkersOnMap > halfCount) {
                val evenDistWeight =
                    (maxMarkersOnMap - halfCount).toDouble() / (fullCount - halfCount)
                val south = lerpLatitude(halfSw.latitude, sw.latitude, evenDistWeight)
                val north = lerpLatitude(halfNe.latitude, ne.latitude, evenDistWeight)
                val west = lerpLongitude(halfSw.longitude, sw.longitude, evenDistWeight, true)
                val east = lerpLongitude(halfNe.longitude, ne.longitude, evenDistWeight, false)
                sw = LatLng(south, west)
                ne = LatLng(north, east)
                // TODO How to best determine count?
            } else {
                sw = halfSw
                ne = halfNe
                queryCount = halfCount
            }
        }

        return BoundsQueryParams(fullCount, queryCount, sw, ne)
    }

    suspend fun queryWorksitesInBounds(
        incidentId: Long,
        boundsSw: LatLng,
        boundsNe: LatLng,
    ): Pair<List<WorksiteMapMark>, Int> = coroutineScope {
        // TODO Make dynamic based on app performance as well
        val maxMarkersOnMap = (2 * appMemoryStats.availableMemory).coerceAtLeast(64)
        val middle = getMiddleCoordinate(boundsSw, boundsNe)
        val q = getBoundQueryParams(
            maxMarkersOnMap,
            incidentId,
            boundsSw,
            boundsNe,
            middle,
        )

        ensureActive()

        val sw = q.southwest
        val ne = q.northeast
        val mapMarks = worksitesRepository.getWorksitesMapVisual(
            incidentId,
            sw.latitude,
            ne.latitude,
            sw.longitude,
            ne.longitude,
            // TODO Review if this is sufficient and mostly complete
            q.queryCount.coerceAtMost(2 * maxMarkersOnMap),
            0,
        )

        ensureActive()

        val mLatRad = middle.latitude.radians
        val mLngRad = middle.longitude.radians
        val midR = sin(mLatRad)
        val midX = midR * cos(mLngRad)
        val midY = midR * sin(mLngRad)
        val midZ = cos(mLatRad)
        fun approxDistanceFromMiddle(latitude: Double, longitude: Double): Double {
            val latRad = latitude.radians
            val lngRad = longitude.radians
            val r = sin(latRad)
            val x = r * cos(lngRad)
            val y = r * sin(lngRad)
            val z = cos(latRad)
            return (x - midX).pow(2.0) + (y - midY).pow(2.0) + (z - midZ).pow(2.0)
        }

        val distanceToMiddleSorted = mapMarks.mapIndexed { index, mark ->
            val distanceMeasure = approxDistanceFromMiddle(mark.latitude, mark.longitude)
            MarkerFromCenter(
                index,
                mark,
                mark.latitude - middle.latitude,
                mark.longitude - middle.longitude,
                distanceMeasure,
            )
        }
            .sortedWith { a, b -> if (a.distanceMeasure - b.distanceMeasure <= 0) -1 else 1 }

        val endIndex = distanceToMiddleSorted.size.coerceAtMost(maxMarkersOnMap)
        val marks = distanceToMiddleSorted
            .slice(0 until endIndex)
            .sortedWith { a, b -> if (a.sortOrder <= b.sortOrder) -1 else 1 }
            .map(MarkerFromCenter::mark)

        ensureActive()

        Pair(marks, q.fullCount)
    }
}

private data class BoundsQueryParams(
    val fullCount: Int,
    val queryCount: Int,
    val southwest: LatLng,
    val northeast: LatLng,
)

private data class MarkerFromCenter(
    val sortOrder: Int,
    val mark: WorksiteMapMark,
    val deltaLatitude: Double,
    val deltaLongitude: Double,
    val distanceMeasure: Double,
)
