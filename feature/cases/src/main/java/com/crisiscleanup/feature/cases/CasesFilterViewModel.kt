package com.crisiscleanup.feature.cases

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.PermissionStatus
import com.crisiscleanup.core.common.locationPermissionGranted
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.commoncase.model.FormFieldNode
import com.crisiscleanup.core.commoncase.model.WorkFormGroupKey
import com.crisiscleanup.core.commoncase.model.flatten
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.CasesFilterRepository
import com.crisiscleanup.core.data.repository.CrisisCleanupWorkTypeStatusRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LanguageTranslationsRepository
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.WorksiteFlagType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

@HiltViewModel
class CasesFilterViewModel @Inject constructor(
    workTypeStatusRepository: CrisisCleanupWorkTypeStatusRepository,
    private val casesFilterRepository: CasesFilterRepository,
    incidentSelector: IncidentSelector,
    incidentsRepository: IncidentsRepository,
    languageRepository: LanguageTranslationsRepository,
    private val permissionManager: PermissionManager,
    val translator: KeyResourceTranslator,
    @Logger(CrisisCleanupLoggers.Cases) private val logger: AppLogger,
) : ViewModel() {
    var showExplainPermissionLocation by mutableStateOf(false)

    val casesFilters = MutableStateFlow(casesFilterRepository.casesFilters.value)

    val workTypeStatuses = workTypeStatusRepository.workTypeStatusFilterOptions

    val worksiteFlags = WorksiteFlagType.values().sortedBy { it.literal }

    val workTypes = incidentSelector.incidentId
        .flatMapLatest { id ->
            incidentsRepository.streamIncident(id)
        }
        .map { incident ->
            incident?.formFields?.let { formFields ->
                val formFieldRootNode = FormFieldNode.buildTree(
                    formFields,
                    languageRepository
                )
                    .map(FormFieldNode::flatten)

                return@map formFieldRootNode.firstOrNull { it.fieldKey == WorkFormGroupKey }
                    ?.let { node ->
                        node.children.filter { it.parentKey == WorkFormGroupKey }
                            .map { it.formField.selectToggleWorkType }
                            .sortedBy { it }
                    }
                    ?: emptyList()
            }
            emptyList()
        }

    val distanceOptions = listOf(
        Pair(0f, translator("worksiteFilters.any_distance")),
        Pair(0.3f, translator("worksiteFilters.point_3_miles")),
        Pair(1f, translator("worksiteFilters.one_mile")),
        Pair(5f, translator("worksiteFilters.five_miles")),
        Pair(20f, translator("worksiteFilters.twenty_miles")),
        Pair(50f, translator("worksiteFilters.fifty_miles")),
        Pair(100f, translator("worksiteFilters.one_hundred_miles")),
    )

    private var distanceOptionCached = AtomicReference<Float?>(null)

    init {
        permissionManager.permissionChanges
            .map {
                if (it == locationPermissionGranted) {
                    changeDistanceFilter()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun changeDistanceFilter() {
        distanceOptionCached.getAndSet(null)?.let {
            changeDistanceFilter(it)
        }
    }

    private fun changeDistanceFilter(distance: Float) {
        changeFilters(casesFilters.value.copy(distance = distance))
    }

    fun tryChangeDistanceFilter(distance: Float) {
        when (permissionManager.requestLocationPermission()) {
            PermissionStatus.Granted -> {
                changeDistanceFilter(distance)
                return
            }

            PermissionStatus.ShowRationale -> {
                showExplainPermissionLocation = true
            }

            PermissionStatus.Requesting,
            PermissionStatus.Denied,
            PermissionStatus.Undefined -> {
                // Ignore these statuses as they're not important
            }
        }

        distanceOptionCached.set(distance)
    }

    fun changeFilters(filters: CasesFilter) {
        casesFilters.value = filters
    }

    fun clearFilters() {
        val filters = CasesFilter()
        changeFilters(filters)
        applyFilters(filters)
    }

    fun applyFilters(filters: CasesFilter) {
        casesFilterRepository.changeFilters(filters)
    }
}

enum class CollapsibleFilterSection {
    Distance,
    General,
    PersonalInfo,
    Flags,
    Work,
    Dates,
}
