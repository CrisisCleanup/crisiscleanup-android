package com.crisiscleanup.feature.cases.map

import android.graphics.Bitmap
import android.graphics.Canvas
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.mapmarker.MapCaseDotProvider
import com.crisiscleanup.core.mapmarker.model.TileCoordinates
import com.crisiscleanup.core.mapmarker.tiler.BorderTile
import com.crisiscleanup.core.mapmarker.tiler.squareBitmap
import com.crisiscleanup.feature.cases.CasesConstant.InteractiveZoomLevel
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.google.android.gms.maps.model.TileProvider.NO_TILE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapLatest
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicLong
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

    fun setIncident(id: Long)

    fun enableTileBoundaries()

    /**
     * @return true when this zoom level renders tiles, false otherwise.
     */
    fun rendersAt(zoom: Float): Boolean

    fun setRenderState(skipRendering: Boolean, zoom: Float)
}

@Singleton
class CaseDotsMapTileRenderer @Inject constructor(
    resourceProvider: AndroidResourceProvider,
    private val worksitesRepository: WorksitesRepository,
    private val mapCaseDotProvider: MapCaseDotProvider,
    private val appEnv: AppEnv,
) : CasesOverviewMapTileRenderer, TileProvider {
    private var renderingCount = MutableStateFlow(0)
    override var isBusy = renderingCount.mapLatest { it > 0 }
        private set

    private val tileSizeDp = 256f
    private val tileSizePx = resourceProvider.dpToPx(tileSizeDp).roundToInt()

    // zoom 9 = 512x512 tiles
    override var zoomThreshold = InteractiveZoomLevel

    private var incidentIdCache = AtomicLong(-1)

    // For visualizing tile boundaries in dev
    private var isRenderingBorder = false
    private val borderTile = BorderTile(tileSizePx)

    private var skipTileRendering = false
    private var mapZoomLevel = 0f

    override fun setRenderState(skipRendering: Boolean, zoom: Float) {
        skipTileRendering = skipRendering
        mapZoomLevel = zoom
    }

    override fun rendersAt(zoom: Float): Boolean =
        // Lower zoom is far out, higher zoom is closer in
        zoom < zoomThreshold + 1

    override fun setIncident(id: Long) {
        incidentIdCache.set(id)
    }

    override fun enableTileBoundaries() {
        if (!appEnv.isDebuggable) {
            return
        }

        isRenderingBorder = true
    }

    override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
        if (skipTileRendering ||
            zoom > zoomThreshold ||
            mapZoomLevel < zoom ||
            mapZoomLevel >= zoom + 1
        ) {
            return NO_TILE
        }

        val coordinates = TileCoordinates(x, y, zoom)
        return renderTile(coordinates)
    }

    private fun renderTile(
        coordinates: TileCoordinates
    ): Tile? {
        try {
            renderingCount.value++
            return renderTileInternal(coordinates)
        } finally {
            renderingCount.value--
        }
    }

    private fun renderTileInternal(
        coordinates: TileCoordinates
    ): Tile? {
        val incidentId = incidentIdCache.get()

        val worksitesCount = worksitesRepository.getWorksitesCount(incidentId)
        if (worksitesCount == 0 || incidentId != incidentIdCache.get()) {
            return null
        }

        var bitmap = renderTile(incidentId, worksitesCount, coordinates)

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
        if (incidentId != incidentIdCache.get()) {
            return null
        }

        return tile
    }

    private fun renderTile(
        incidentId: Long,
        worksitesCount: Int,
        coordinates: TileCoordinates,
    ): Bitmap? {
        val limit = 2000
        var offset = 0
        val centerDotOffset = -mapCaseDotProvider.centerSizePx

        val sw = coordinates.querySouthwest
        val ne = coordinates.queryNortheast

        var bitmap: Bitmap? = null
        var canvas: Canvas? = null

        for (i in 0 until worksitesCount step limit) {
            val worksites = worksitesRepository.getWorksitesMapVisual(
                incidentId,
                sw.latitude,
                ne.latitude,
                sw.longitude,
                ne.longitude,
                limit,
                offset,
            )

            if (worksites.isNotEmpty() && bitmap == null) {
                bitmap = if (isRenderingBorder) borderTile.copy()
                else squareBitmap(tileSizePx)
                canvas = Canvas(bitmap)
            }

            worksites.onEach {
                mapCaseDotProvider.getDotBitmap(it.statusClaim)?.let { dotBitmap ->
                    val xyNorm = coordinates.fromLatLng(it.latitude, it.longitude)
                    if (xyNorm == null) {
                        // TODO This needs finishing (and is not working as designed).
                        //      Worksites on the outskirts of the tile (centered in neighboring tiles) need to be drawn overlapping at the boundaries and complement the dot visual of the worksite.
                    } else {
                        val (xNorm, yNorm) = xyNorm
                        val left = xNorm.toFloat() * tileSizePx + centerDotOffset
                        val top = yNorm.toFloat() * tileSizePx + centerDotOffset
                        canvas!!.drawBitmap(dotBitmap, left, top, null)
                    }
                }
            }

            // There are no more worksites in this tile
            if (worksites.size < limit) {
                break
            }

            offset += limit
        }

        return bitmap
    }
}
