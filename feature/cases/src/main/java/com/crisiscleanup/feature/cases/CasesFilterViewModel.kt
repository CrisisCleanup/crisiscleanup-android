package com.crisiscleanup.feature.cases

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.PermissionStatus
import com.crisiscleanup.core.common.locationPermissionGranted
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.commoncase.model.FormFieldNode
import com.crisiscleanup.core.commoncase.model.WORK_FORM_GROUP_KEY
import com.crisiscleanup.core.commoncase.model.flatten
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.di.CasesFilterType
import com.crisiscleanup.core.data.di.CasesFilterTypes
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.CasesFilterRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LanguageTranslationsRepository
import com.crisiscleanup.core.data.repository.OrganizationsRepository
import com.crisiscleanup.core.data.repository.WorkTypeStatusRepository
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.WorksiteFlagType
import com.crisiscleanup.feature.cases.navigation.CasesFilterArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

@HiltViewModel
class CasesFilterViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    workTypeStatusRepository: WorkTypeStatusRepository,
    @CasesFilterType(CasesFilterTypes.Cases)
    casesFilterRepository: CasesFilterRepository,
    @CasesFilterType(CasesFilterTypes.TeamCases)
    teamCasesFilterRepository: CasesFilterRepository,
    incidentSelector: IncidentSelector,
    incidentsRepository: IncidentsRepository,
    languageRepository: LanguageTranslationsRepository,
    accountDataRepository: AccountDataRepository,
    organizationsRepository: OrganizationsRepository,
    private val permissionManager: PermissionManager,
    translator: KeyResourceTranslator,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.Cases) private val logger: AppLogger,
) : ViewModel() {
    private val casesFilterArgs = CasesFilterArgs(savedStateHandle)
    private val useTeamFilters = casesFilterArgs.useTeamFilters

    private val filterRepository = if (useTeamFilters) {
        teamCasesFilterRepository
    } else {
        casesFilterRepository
    }

    var showExplainPermissionLocation by mutableStateOf(false)

    // TODO This requires the filters were previously accessed
    val casesFilters = MutableStateFlow(filterRepository.casesFilters)

    val hasInconsistentDistanceFilter = combine(
        permissionManager.hasLocationPermission,
        casesFilters,
    ) { hasPermission, filters ->
        filters.hasDistanceFilter && !hasPermission
    }

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
                    languageRepository,
                )
                    .map(FormFieldNode::flatten)

                return@map formFieldRootNode.firstOrNull { it.fieldKey == WORK_FORM_GROUP_KEY }
                    ?.let { node ->
                        node.children.filter { it.parentKey == WORK_FORM_GROUP_KEY }
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

    val hasOrganizationAreas = accountDataRepository.accountData
        .filter { it.org.id > 0 }
        .flatMapLatest {
            organizationsRepository.streamPrimarySecondaryAreas(it.org.id)
        }
        .map { Pair(it.primary != null, it.secondary != null) }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = Pair(false, false),
            started = SharingStarted.WhileSubscribed(),
        )

    init {
        permissionManager.permissionChanges
            .onEach {
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

            PermissionStatus.Requesting -> {
                distanceOptionCached.set(distance)
            }

            PermissionStatus.Denied,
            PermissionStatus.Undefined,
            -> {
                // Ignore these statuses as they're not important
            }
        }

        distanceOptionCached.set(distance)
    }

    fun onRequestLocationPermission() {
        permissionManager.requestLocationPermission()
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
        filterRepository.changeFilters(filters)
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
