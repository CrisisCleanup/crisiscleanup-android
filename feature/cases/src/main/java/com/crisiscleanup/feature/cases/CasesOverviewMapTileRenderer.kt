package com.crisiscleanup.feature.cases

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.collection.LruCache
import androidx.compose.runtime.mutableStateOf
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.mapmarker.MapCaseDotProvider
import com.crisiscleanup.core.mapmarker.model.TileCoordinates
import com.crisiscleanup.core.mapmarker.tiler.makeTileBorderBitmap
import com.crisiscleanup.feature.cases.CasesConstant.InteractiveZoomLevel
import com.crisiscleanup.feature.cases.model.EmptyMapTileCases
import com.crisiscleanup.feature.cases.model.MapTileCases
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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

    // TODO Is it possible to inject?
    fun setScope(coroutineScope: CoroutineScope)

    /**
     * @return true when this zoom level renders tiles, false otherwise.
     */
    fun rendersAt(zoom: Float): Boolean

    fun setRendering(render: Boolean)
}

@Singleton
class CaseDotsMapTileRenderer @Inject constructor(
    resourceProvider: AndroidResourceProvider,
    private val worksitesRepository: WorksitesRepository,
    private val mapCaseDotProvider: MapCaseDotProvider,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : CasesOverviewMapTileRenderer, TileProvider {
    private var renderingCount = MutableStateFlow(0)
    override var isBusy = renderingCount.mapLatest { it > 0 }
        private set

    private val tileSizeDp: Float = 256f
    private val tileSizePx: Int = resourceProvider.dpToPx(tileSizeDp).roundToInt()

    // zoom 9 = 512x512 tiles
    override var zoomThreshold: Int = InteractiveZoomLevel

    private var incidentIdCache: Long = -1
    private val cache = MapTileCasesLruCache(2f)

    private var coroutineScope: CoroutineScope? = null

    // For visualizing tile boundaries in dev
    private val borderTile: Bitmap = makeTileBorderBitmap(tileSizePx)

    /**
     * Size of tiles data
     *
     * Observe for changes in tiles generated. Not exact but good enough to signify changes.
     */
    var tileDataSize = mutableStateOf(0)
        private set

    private var isRenderingTiles = false

    override fun setRendering(render: Boolean) {
        isRenderingTiles = render
    }

    override fun rendersAt(zoom: Float): Boolean =
        // Lower zoom is far out, higher zoom is closer in
        coroutineScope != null && zoom < zoomThreshold + 1

    override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
        if (!isRenderingTiles || zoom > zoomThreshold) {
            return null
        }

        val coordinates = TileCoordinates(x, y, zoom)

        // Always try and render tile in case update is needed.
        // Cache changes should be observed and UI updated as necessary.
        coroutineScope?.launch {
            renderTile(coordinates)
        }

        return cache.get(coordinates)?.tile
    }

    override fun setIncident(id: Long) {
        synchronized(cache) {
            if (id != incidentIdCache) {
                incidentIdCache = id
                cache.evictAll()
                tileDataSize.value = cache.size()
            }
        }
    }

    override fun setScope(coroutineScope: CoroutineScope) {
        this.coroutineScope = coroutineScope
    }

    private suspend fun renderTile(
        coordinates: TileCoordinates
    ): Unit = withContext(ioDispatcher) {
        try {
            renderingCount.value++
            renderTileInternal(coordinates)
        } finally {
            renderingCount.value--
        }
    }

    private suspend fun renderTileInternal(
        coordinates: TileCoordinates
    ) = coroutineScope {
        val incidentId = incidentIdCache

        val worksitesCount = worksitesRepository.getWorksitesCount(incidentId)
        if (worksitesCount == 0) {
            synchronized(cache) {
                cache.put(coordinates, EmptyMapTileCases)
                tileDataSize.value = cache.size()
                return@coroutineScope
            }
        }

        var updateTile = true
        cache.get(coordinates)?.let {
            // There are edge cases where this may not hold but is not important when the map is zoomed this far out
            updateTile = it.caseCount != worksitesCount
        }
        if (!updateTile || incidentId != incidentIdCache || !isActive) {
            return@coroutineScope
        }

        renderTile(incidentId, worksitesCount, coordinates)?.let { bitmap ->
            if (!isActive) {
                return@coroutineScope
            }

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream)
            val bitmapData = stream.toByteArray()
            val tile = Tile(tileSizePx, tileSizePx, bitmapData)
            val mapTileCases = MapTileCases(
                coordinates.southwest,
                coordinates.northeast,
                worksitesCount,
                tile,
            )

            synchronized(cache) {
                if (incidentId != incidentIdCache || !isActive) {
                    return@coroutineScope
                }

                cache.put(coordinates, mapTileCases)
                tileDataSize.value = cache.size()
            }
        }
    }

    private suspend fun renderTile(
        incidentId: Long,
        worksitesCount: Int,
        coordinates: TileCoordinates,
    ): Bitmap? = coroutineScope {
        val bitmap = Bitmap.createBitmap(tileSizePx, tileSizePx, Bitmap.Config.ARGB_8888)
//        val bitmap = copyBorderTile(borderTile)

        val canvas = Canvas(bitmap!!)

        val limit = 2000
        var offset = 0
        val centerDotOffset = -mapCaseDotProvider.centerSizePx

        val sw = coordinates.southwest
        val ne = coordinates.northeast
        val boundsPadding = coordinates.boundsPadding
        val latitudeSouth = sw.latitude - boundsPadding
        val latitudeNorth = ne.latitude + boundsPadding
        val longitudeWest = (sw.longitude - boundsPadding).coerceAtLeast(-180.0)
        val longitudeEast = (ne.longitude + boundsPadding).coerceAtMost(180.0)

        for (i in 0 until worksitesCount step limit) {
            if (!isActive) {
                return@coroutineScope null
            }

            val worksites = worksitesRepository.getWorksitesMapVisual(
                incidentId,
                latitudeSouth,
                latitudeNorth,
                longitudeWest,
                longitudeEast,
                limit,
                offset,
            )

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
                        canvas.drawBitmap(dotBitmap, left, top, null)
                    }
                }
            }

            // There are no more worksites in this tile
            if (worksites.size < limit) {
                break
            }

            offset += limit
        }

        return@coroutineScope bitmap
    }
}

private class MapTileCasesLruCache(maxSizeMb: Float) :
    ContentSizeLruCache<TileCoordinates, MapTileCases>(maxSizeMb) {
    override fun sizeOf(value: MapTileCases): Int = value.tile?.data?.size ?: 0
}

abstract class ContentSizeLruCache<K : Any, V : Any> constructor(maxSizeMb: Float) :
    LruCache<K, V>((maxSizeMb * 1024 * 1024).toInt()) {
    abstract fun sizeOf(value: V): Int

    override fun sizeOf(key: K, value: V): Int = sizeOf(value)
}

@Module
@InstallIn(SingletonComponent::class)
interface OverviewMapTileRendererModule {
    @Singleton
    @Binds
    fun bindsTileRenderer(renderer: CaseDotsMapTileRenderer): CasesOverviewMapTileRenderer

    @Singleton
    @Binds
    fun bindsTileProvider(renderer: CaseDotsMapTileRenderer): TileProvider
}
