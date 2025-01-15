package com.crisiscleanup.sandbox

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.WorkTypeStatusClaim
import com.crisiscleanup.core.model.data.WorkTypeType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class MapMarkersViewModel @Inject constructor(
    iconProvider: MapCaseIconProvider,
    @Dispatcher(CrisisCleanupDispatchers.IO) ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val mapMarkers = MutableStateFlow<List<Bitmap?>>(emptyList())

    init {
        viewModelScope.launch(ioDispatcher) {
            val statuses = WorkTypeStatus.entries
            val workTypes = WorkTypeType.entries

            val icons = mutableListOf<Bitmap?>()
            for (wt in 0..<workTypes.size) {
                for (s in 0..<statuses.size) {
                    val isClaimed = Random.nextFloat() < 0.3f
                    val hasMultiple = Random.nextFloat() < 0.2f
                    val isDuplicate = Random.nextFloat() < 0.1f
                    val isFilteredOut = Random.nextFloat() < 0.1f
                    val isVisited = Random.nextFloat() < 0.2f
                    val isTeamAssigned = Random.nextFloat() < 0.3f
                    val bitmap = iconProvider.getIconBitmap(
                        statusClaim = WorkTypeStatusClaim(
                            statuses[s],
                            isClaimed,
                        ),
                        workType = workTypes[wt],
                        hasMultipleWorkTypes = hasMultiple,
                        isDuplicate = isDuplicate && !isFilteredOut,
                        isFilteredOut = isFilteredOut,
                        isVisited = isVisited,
                        isAssignedTeam = isTeamAssigned,
                    )
                    icons.add(bitmap)
                }
            }
            mapMarkers.value = icons
        }
    }
}
