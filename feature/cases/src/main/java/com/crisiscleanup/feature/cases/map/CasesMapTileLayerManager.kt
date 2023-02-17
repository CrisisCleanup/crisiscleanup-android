package com.crisiscleanup.feature.cases.map

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.crisiscleanup.core.common.throttleLatest
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.model.data.EmptyIncident
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Signals when the Compose map tile layer should invalidate its cache and/or redraw new or updated tiles.
 *
 * The complexity stems from the variations of OS/Google map versions, limited memory, realtime changes in data and states.
 */
internal class CasesMapTileLayerManager(
    coroutineScope: CoroutineScope,
    private val incidentSelector: IncidentSelector,
    private val worksitesRepository: WorksitesRepository,
    private val mapBoundsManager: CasesMapBoundsManager,
) {
    private var tileDataChangeKey = mutableStateOf(0L)

    /**
     * Indicates the Compose map should refresh as tile data has changed in a significant manner
     */
    val overviewTileDataChange: State<Long> = tileDataChangeKey

    private val incidentWorksitesCount = incidentSelector.incidentId.flatMapLatest {
        worksitesRepository.streamIncidentWorksitesCount(it)
    }.shareIn(
        scope = coroutineScope,
        replay = 1,
        started = SharingStarted.WhileSubscribed(1_000)
    )

    // TODO Search for updated documentation on a more elegant technique once clearTileCache has been available for some time.
    private var _clearTileLayer: Boolean = false

    /**
     * Take state indicating the tile layer should be invalidated/cleared
     */
    var clearTileLayer: Boolean
        get() {
            if (_clearTileLayer) {
                _clearTileLayer = false
                return true
            }
            return false
        }
        private set(value) {
            if (value) {
                if (incidentSelector.incidentId.value != EmptyIncident.id && mapBoundsManager.isMapLoaded) {
                    _clearTileLayer = true
                }
            } else {
                _clearTileLayer = false
            }
        }

    private val hideTiling = AtomicBoolean(true)
    private val zoomLevelCache = AtomicInteger(-1)

    init {
        incidentSelector.incidentId
            .onEach {
                clearTileLayer = true
                onTileChange(it)
            }
            .launchIn(coroutineScope)

        incidentWorksitesCount
            .throttleLatest(2000)
            .onEach {
                clearTileLayer = true
                val key = incidentSelector.incidentId.value + it
                onTileChange(key)
            }
            .launchIn(coroutineScope)
    }

    fun setTilingState(isTilingHidden: Boolean, zoom: Float) {
        if (hideTiling.getAndSet(isTilingHidden) != isTilingHidden && !isTilingHidden) {
            clearTiles()
        }

        val zoomFloor = floor(zoom).roundToInt()
        if (zoomLevelCache.getAndSet(zoomFloor) != zoomFloor) {
            clearTiles()
        }
    }

    fun clearTiles() {
        clearTileLayer = true
        onTileChange()
    }

    // TODO Develop a change mechanism that guarantees uniqueness
    private fun onTileChange() = onTileChange(tileDataChangeKey.value + 1)

    private fun onTileChange(dataChangeValue: Long) {
        if (hideTiling.get()) {
            return
        }

        tileDataChangeKey.value = dataChangeValue % 1_000_000
    }
}