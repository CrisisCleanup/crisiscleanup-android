package com.crisiscleanup.feature.cases.map

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.model.data.EmptyIncident
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.time.Clock

/**
 * Signals when the Compose map tile layer should invalidate its cache and/or redraw new or updated tiles.
 *
 * The complexity stems from the variations of OS/Google map versions, limited memory, realtime changes in data and states.
 */
internal class CasesMapTileLayerManager(
    coroutineScope: CoroutineScope,
    private val incidentSelector: IncidentSelector,
    private val mapBoundsManager: CasesMapBoundsManager,
    private val logger: AppLogger,
) {
    private var tileDataChangeKey = mutableLongStateOf(0L)

    /**
     * Indicates the Compose map should refresh as tile data has changed in a significant manner
     */
    val overviewTileDataChange: State<Long> = tileDataChangeKey

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
                if (incidentSelector.incidentId.value != EmptyIncident.id &&
                    mapBoundsManager.isMapLoaded
                ) {
                    _clearTileLayer = true
                }
            } else {
                _clearTileLayer = false
            }
        }

    init {
        incidentSelector.incidentId
            .onEach {
                clearTileLayer = true
                onTileChange(it)
            }
            .launchIn(coroutineScope)
    }

    fun clearTiles() {
        clearTileLayer = true
        onTileChange()
    }

    // TODO Develop a change mechanism that guarantees uniqueness from any change
    private fun onTileChange() = onTileChange(Clock.System.now().toEpochMilliseconds())

    private fun onTileChange(dataChangeValue: Long) {
        tileDataChangeKey.longValue = dataChangeValue
    }
}
