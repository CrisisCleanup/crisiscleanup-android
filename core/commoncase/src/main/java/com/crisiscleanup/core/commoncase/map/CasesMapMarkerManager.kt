package com.crisiscleanup.core.commoncase.map

import com.crisiscleanup.core.common.AppMemoryStats
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.radians
import com.crisiscleanup.core.commoncase.CasesConstant.MAP_MARKERS_ZOOM_LEVEL
import com.crisiscleanup.core.commoncase.model.CoordinateBounds
import com.crisiscleanup.core.commoncase.model.CoordinateUtil.getMiddleCoordinate
import com.crisiscleanup.core.commoncase.model.CoordinateUtil.lerpLatitude
import com.crisiscleanup.core.commoncase.model.CoordinateUtil.lerpLongitude
import com.crisiscleanup.core.commoncase.model.asWorksiteGoogleMapMark
import com.crisiscleanup.core.data.WorksiteInteractor
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

class CasesMapMarkerManager(
    private val isTeamCasesMap: Boolean,
    private val worksitesRepository: WorksitesRepository,
    worksiteQueryState: StateFlow<CasesQueryState>,
    mapBoundsManager: CasesMapBoundsManager,
    worksiteInteractor: WorksiteInteractor,
    mapCaseIconProvider: MapCaseIconProvider,
    private val appMemoryStats: AppMemoryStats,
    private val locationProvider: LocationProvider,
    coroutineScope: CoroutineScope,
    coroutineDispatcher: CoroutineDispatcher,
) {
    private val isGeneratingWorksiteMarkersInternal = MutableStateFlow(false)
    val isGeneratingWorksiteMarkers: StateFlow<Boolean> = isGeneratingWorksiteMarkersInternal

    @OptIn(FlowPreview::class)
    val worksitesMapMarkers = combine(
        worksiteQueryState,
        mapBoundsManager.isMapLoaded,
        ::Pair,
    )
        // TODO Make delay a parameter
        .debounce(250)
        .mapLatest { (wqs, isMapLoaded) ->
            val id = wqs.incidentId

            val skipMarkers = !isMapLoaded ||
                wqs.isNotMapView ||
                id == EmptyIncident.id ||
                wqs.zoom < MAP_MARKERS_ZOOM_LEVEL

            if (skipMarkers) {
                emptyList()
            } else {
                // TODO Atomic update
                isGeneratingWorksiteMarkersInternal.value = true
                try {
                    generateWorksiteMarkers(
                        id,
                        wqs.coordinateBounds,
                        wqs.zoom,
                        worksiteInteractor,
                        mapCaseIconProvider,
                        wqs.teamCaseIds,
                    )
                } finally {
                    isGeneratingWorksiteMarkersInternal.value = false
                }
            }
        }
        .flowOn(coroutineDispatcher)
        .stateIn(
            scope = coroutineScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

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
            locationProvider.getLocation(),
            useTeamFilters = isTeamCasesMap,
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

    private val zeroOffset = Pair(0f, 0f)

    private val denseMarkCountThreshold = 15
    private val denseMarkZoomThreshold = 14
    private val denseDegreeThreshold = 0.0001
    private val denseScreenOffsetScale = 0.6f
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
                    val offsetScale = denseScreenOffsetScale + (count - 5).coerceAtLeast(0) * 0.2f
                    if (count > 1) {
                        var offsetDir = (PI * 0.5).toFloat()
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

    private suspend fun CasesMapMarkerManager.generateWorksiteMarkers(
        incidentId: Long,
        coordinateBounds: CoordinateBounds,
        mapZoom: Float,
        worksiteInteractor: WorksiteInteractor,
        mapCaseIconProvider: MapCaseIconProvider,
        teamCaseIds: Set<Long>,
    ) = coroutineScope {
        val sw = coordinateBounds.southWest
        val ne = coordinateBounds.northEast
        val marksQuery = queryWorksitesInBounds(incidentId, sw, ne)
        val marks = marksQuery.first
        val markOffsets = denseMarkerOffsets(marks, mapZoom)

        ensureActive()

        val now = Clock.System.now()
        marks.mapIndexed { index, mark ->
            val offset = if (index < markOffsets.size) {
                markOffsets[index]
            } else {
                zeroOffset
            }
            val isSelected = worksiteInteractor.wasCaseSelected(
                incidentId,
                worksiteId = mark.id,
                reference = now,
            )
            val isAssignedTeam = teamCaseIds.contains(mark.id)
            mark.asWorksiteGoogleMapMark(
                mapCaseIconProvider,
                isVisited = isSelected,
                isAssignedTeam = isAssignedTeam,
                offset,
            )
        }
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
