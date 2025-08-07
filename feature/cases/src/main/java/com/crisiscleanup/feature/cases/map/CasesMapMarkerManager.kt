package com.crisiscleanup.feature.cases.map

import com.crisiscleanup.core.common.AppMemoryStats
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.radians
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.crisiscleanup.feature.cases.CasesConstant
import com.crisiscleanup.feature.cases.map.CoordinateUtil.getMiddleCoordinate
import com.crisiscleanup.feature.cases.map.CoordinateUtil.lerpLatitude
import com.crisiscleanup.feature.cases.map.CoordinateUtil.lerpLongitude
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

internal class CasesMapMarkerManager(
    private val worksitesRepository: WorksitesRepository,
    private val appMemoryStats: AppMemoryStats,
    private val locationProvider: LocationProvider,
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

        return BoundsQueryParams(
            fullCount = fullCount,
            queryCount = queryCount,
            southWest = sw,
            northEast = ne,
        )
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

        val sw = q.southWest
        val ne = q.northEast
        val mapMarks = worksitesRepository.getWorksitesMapVisual(
            incidentId,
            sw.latitude,
            ne.latitude,
            sw.longitude,
            ne.longitude,
            // TODO Review if this is sufficient and mostly complete
            q.queryCount.coerceAtMost(2 * maxMarkersOnMap),
            0,
            locationProvider.getLocation(),
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
            .sortedWith { a, b ->
                val deltaDistance = a.distanceMeasure - b.distanceMeasure
                if (deltaDistance < 0) {
                    -1
                } else if (deltaDistance > 0) {
                    1
                } else {
                    if (a.mark.id < b.mark.id) -1 else 1
                }
            }

        val endIndex = distanceToMiddleSorted.size.coerceAtMost(maxMarkersOnMap)
        val marks = distanceToMiddleSorted
            .slice(0 until endIndex)
            .sortedWith { a, b -> if (a.sortOrder <= b.sortOrder) -1 else 1 }
            .map(MarkerFromCenter::mark)

        ensureActive()

        Pair(marks, q.fullCount)
    }

    val zeroOffset = Pair(0f, 0f)

    private val denseMarkCountThreshold = 15
    private val denseMarkZoomThreshold = CasesConstant.MAP_MARKERS_ZOOM_LEVEL + 4
    private val denseDegreeThreshold = 0.0001
    private val denseScreenOffsetScale = 1.2f
    suspend fun denseMarkerOffsets(
        marks: List<WorksiteMapMark>,
        zoom: Float,
    ): List<Pair<Float, Float>> =
        coroutineScope {
            if (marks.size > denseMarkCountThreshold ||
                zoom < denseMarkZoomThreshold
            ) {
                return@coroutineScope emptyList()
            }

            ensureActive()

            val bucketIndices = IntArray(marks.size) { -1 }
            val buckets = mutableListOf<MutableList<Int>>()
            for (i in 0 until marks.size - 1) {
                val iMark = marks[i]
                for (j in i + 1 until marks.size) {
                    val jMark = marks[j]
                    if (abs(iMark.latitude - jMark.latitude) < denseDegreeThreshold &&
                        abs(iMark.longitude - jMark.longitude) < denseDegreeThreshold
                    ) {
                        val bucketI = bucketIndices[i]
                        if (bucketI >= 0) {
                            bucketIndices[j] = bucketI
                            buckets[bucketI].add(j)
                        } else {
                            val bucketJ = bucketIndices[j]
                            if (bucketJ >= 0) {
                                bucketIndices[i] = bucketJ
                                buckets[bucketJ].add(i)
                            } else {
                                val bucketIndex = buckets.size
                                bucketIndices[i] = bucketIndex
                                bucketIndices[j] = bucketIndex
                                buckets.add(mutableListOf(i, j))
                            }
                        }
                        break
                    }
                }
                ensureActive()
            }

            val markOffsets = marks.map { zeroOffset }.toMutableList()
            if (buckets.isNotEmpty()) {
                buckets.forEach {
                    val count = it.size
                    if (count > 1) {
                        var offsetDir = (PI * 0.5).toFloat()
                        val offsetScale =
                            denseScreenOffsetScale + (count - 5).coerceAtLeast(0) * 0.2f
                        val deltaDirDegrees = (2 * PI / count).toFloat()
                        it.forEach { index ->
                            markOffsets[index] = Pair(
                                offsetScale * cos(offsetDir),
                                offsetScale * sin(offsetDir),
                            )
                            offsetDir += deltaDirDegrees
                        }
                    }
                }
            }
            markOffsets
        }
}

private data class BoundsQueryParams(
    val fullCount: Int,
    val queryCount: Int,
    val southWest: LatLng,
    val northEast: LatLng,
)

private data class MarkerFromCenter(
    val sortOrder: Int,
    val mark: WorksiteMapMark,
    val deltaLatitude: Double,
    val deltaLongitude: Double,
    val distanceMeasure: Double,
)
