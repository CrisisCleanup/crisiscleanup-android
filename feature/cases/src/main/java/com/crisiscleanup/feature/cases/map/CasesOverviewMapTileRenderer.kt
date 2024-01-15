package com.crisiscleanup.feature.cases.map

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.collection.LruCache
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.mapmarker.MapCaseDotProvider
import com.crisiscleanup.core.mapmarker.model.TileCoordinates
import com.crisiscleanup.core.mapmarker.tiler.BorderTile
import com.crisiscleanup.core.mapmarker.tiler.squareBitmap
import com.crisiscleanup.feature.cases.CasesConstant.MapDotsZoomLevel
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.google.android.gms.maps.model.TileProvider.NO_TILE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapLatest
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

interface CasesOverviewMapTileRenderer {
    val isBusy: Flow<Boolean>

    /**
     * Zoom level at which tiles still render
     *
     * Higher zooms will not render any tiles.
     * At a zoom level of 0 there is 1 tile (1x1).
     * At a zoom level of 8 there are 64x64 tiles.
     */
    var zoomThreshold: Int

    /**
     * @return true if incident is changed or false otherwise
     */
    fun setIncident(id: Long, worksitesCount: Int, clearCache: Boolean = true): Boolean

    fun setLocation(coordinates: Pair<Double, Double>?)

    fun enableTileBoundaries()
}

@Singleton
class CaseDotsMapTileRenderer @Inject constructor(
    resourceProvider: AndroidResourceProvider,
    private val worksitesRepository: WorksitesRepository,
    private val mapCaseDotProvider: MapCaseDotProvider,
    private val appEnv: AppEnv,
) : CasesOverviewMapTileRenderer, TileProvider {
    private val renderingCounter = AtomicInteger()
    private var renderingCount = MutableStateFlow(0)
    override var isBusy = renderingCount.mapLatest { it > 0 }
        private set

    private val tileSizeDp = 256f
    private val tileSizePx = resourceProvider.dpToPx(tileSizeDp).roundToInt()

    override var zoomThreshold = MapDotsZoomLevel

    private val tileCache = TileDataCache(1.5f)
    private var incidentIdCache = -1L
    private var worksitesCount = 0

    // For visualizing tile boundaries in dev
    private var isRenderingBorder = false
    private val borderTile = BorderTile(tileSizePx)

    private var locationCoordinates: Pair<Double, Double>? = null

    override fun setIncident(id: Long, worksitesCount: Int, clearCache: Boolean): Boolean {
        val isIncidentChanged = id != incidentIdCache
        synchronized(tileCache) {
            if (isIncidentChanged || clearCache) {
                tileCache.evictAll()
            }
            incidentIdCache = id
            this.worksitesCount = worksitesCount
        }
        return isIncidentChanged
    }

    override fun setLocation(coordinates: Pair<Double, Double>?) {
        locationCoordinates = coordinates
    }

    override fun enableTileBoundaries() {
        if (!appEnv.isDebuggable) {
            return
        }

        isRenderingBorder = true
    }

    override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
        val incidentId = incidentIdCache

        if (zoom > zoomThreshold ||
            worksitesCount == 0
        ) {
            return NO_TILE
        }

        val coordinates = TileCoordinates(x, y, zoom)
        val tileData = tileCache[coordinates]
        tileData?.let { data ->
            if (data.tileCaseCount == 0 || data.tile != null) {
                return data.tile ?: NO_TILE
            }
        }

        val tile = renderTile(coordinates)
        return if (incidentId != incidentIdCache) NO_TILE else tile
    }

    private fun renderTile(
        coordinates: TileCoordinates,
    ): Tile? {
        try {
            renderingCount.value = renderingCounter.incrementAndGet()
            return renderTileInternal(coordinates)
        } finally {
            renderingCount.value = renderingCounter.decrementAndGet()
        }
    }

    private fun renderTileInternal(
        coordinates: TileCoordinates,
    ): Tile? {
        val incidentId = incidentIdCache

        var (boundedWorksitesCount, bitmap) = renderTile(incidentId, worksitesCount, coordinates)

        // Incident has changed this tile is invalid
        if (incidentId != incidentIdCache) {
            return null
        }

        val tile = if (bitmap == null && !isRenderingBorder) {
            NO_TILE
        } else {
            if (bitmap == null) {
                bitmap = borderTile.copy()
            }

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream)
            val bitmapData = stream.toByteArray()
            bitmap.recycle()
            Tile(tileSizePx, tileSizePx, bitmapData)
        }

        synchronized(tileCache) {
            // Incident has changed this tile is invalid
            if (incidentId != incidentIdCache) {
                return null
            }

            val tileData = MapTileCases(
                boundedWorksitesCount,
                worksitesCount,
                tile,
            )
            tileCache.put(coordinates, tileData)
        }

        return tile
    }

    private fun renderTile(
        incidentId: Long,
        worksitesCount: Int,
        coordinates: TileCoordinates,
    ): Pair<Int, Bitmap?> {
        val limit = 2000
        var offset = 0
        val centerDotOffset = -mapCaseDotProvider.iconOffset.x

        val sw = coordinates.querySouthwest
        val ne = coordinates.queryNortheast

        var bitmap: Bitmap? = null
        var canvas: Canvas? = null

        var boundedWorksitesCount = 0

        for (i in 0 until worksitesCount step limit) {
            val worksites = worksitesRepository.getWorksitesMapVisual(
                incidentId,
                sw.latitude,
                ne.latitude,
                sw.longitude,
                ne.longitude,
                limit,
                offset,
                locationCoordinates,
            )

            // Incident has changed this tile is invalid
            if (incidentId != incidentIdCache) {
                break
            }

            if (worksites.isNotEmpty() && bitmap == null) {
                bitmap = if (isRenderingBorder) {
                    borderTile.copy()
                } else {
                    squareBitmap(tileSizePx)
                }
                canvas = Canvas(bitmap)
            }

            worksites.onEach {
                mapCaseDotProvider.getIconBitmap(
                    it.statusClaim,
                    it.workType,
                    it.workTypeCount > 1,
                    it.isDuplicate,
                    it.isFilteredOut,
                )?.let { dotBitmap ->
                    coordinates.fromLatLng(it.latitude, it.longitude)?.let { xyNorm ->
                        val (xNorm, yNorm) = xyNorm
                        val left = xNorm.toFloat() * tileSizePx + centerDotOffset
                        val top = yNorm.toFloat() * tileSizePx + centerDotOffset
                        canvas!!.drawBitmap(dotBitmap, left, top, null)
                    }
                }
            }

            boundedWorksitesCount += worksites.size

            // Incident has changed this tile is invalid
            if (incidentId != incidentIdCache) {
                break
            }

            // There are no more worksites in this tile
            if (worksites.size < limit) {
                break
            }

            offset += limit
        }

        return Pair(boundedWorksitesCount, bitmap)
    }
}

private data class MapTileCases(
    val tileCaseCount: Int,
    val incidentCaseCount: Int,
    val tile: Tile?,
)

private class TileDataCache(sizeMb: Float) :
    DataSizeLruCache<TileCoordinates, MapTileCases>(sizeMb) {
    override fun sizeOf(value: MapTileCases): Int = value.tile?.data?.size ?: 0
}

abstract class DataSizeLruCache<K : Any, V : Any>(sizeMb: Float) :
    LruCache<K, V>((sizeMb * 1_000_000).roundToInt()) {
    abstract fun sizeOf(value: V): Int

    override fun sizeOf(key: K, value: V): Int = sizeOf(value)
}
