package com.crisiscleanup.core.commoncase

import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.commoncase.model.WorksiteGoogleMapMark
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.model.data.IncidentIdWorksiteCount
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

class CasesCounter(
    incidentSelector: IncidentSelector,
    incidentWorksitesCount: Flow<IncidentIdWorksiteCount>,
    isLoadingData: Flow<Boolean>,
    isMapVisible: Flow<Boolean>,
    worksitesMapMarkers: Flow<List<WorksiteGoogleMapMark>>,
    translator: KeyResourceTranslator,
    coroutineScope: CoroutineScope,
    coroutineDispatcher: CoroutineDispatcher,
) {
    val totalCasesCount = combine(
        isLoadingData,
        incidentSelector.incidentId,
        incidentWorksitesCount,
    ) { isLoading, incidentId, worksitesCount ->
        if (incidentId != worksitesCount.id) {
            return@combine -1
        }

        val totalCount = worksitesCount.filteredCount
        if (totalCount == 0 && isLoading) {
            return@combine -1
        }

        totalCount
    }
        .flowOn(coroutineDispatcher)
        .shareIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1,
        )

    val casesCountMapText = combine(
        totalCasesCount,
        isMapVisible,
        worksitesMapMarkers,
        ::Triple,
    )
        .filter { (_, isMapVisible, _) -> isMapVisible }
        .map { (totalCount, _, markers) ->
            if (totalCount < 0) {
                return@map ""
            }

            val visibleCount = markers.filterNot { it.isFilteredOut }.size

            val countText = if (visibleCount == totalCount || visibleCount == 0) {
                if (visibleCount == 0) {
                    translator("info.t_of_t_cases")
                        .replace("{visible_count}", "$totalCount")
                } else if (totalCount == 1) {
                    translator("info.1_of_1_case")
                } else {
                    translator("info.t_of_t_cases")
                        .replace("{visible_count}", "$totalCount")
                }
            } else {
                translator("info.v_of_t_cases")
                    .replace("{visible_count}", "$visibleCount")
                    .replace("{total_count}", "$totalCount")
            }

            countText
        }
        .stateIn(
            scope = coroutineScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )
}
