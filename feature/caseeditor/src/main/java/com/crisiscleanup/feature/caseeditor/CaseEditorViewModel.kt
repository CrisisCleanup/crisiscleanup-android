package com.crisiscleanup.feature.caseeditor

import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.SyncPusher
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.*
import com.crisiscleanup.core.model.data.*
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import com.crisiscleanup.feature.caseeditor.model.coordinates
import com.crisiscleanup.feature.caseeditor.navigation.CaseEditorArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

internal const val DetailsFormGroupKey = "property_info"
internal const val WorkFormGroupKey = "work_info"
internal const val HazardsFormGroupKey = "hazards_info"
internal const val VolunteerReportFormGroupKey = "claim_status_report_info"

@HiltViewModel
class CaseEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    accountDataRepository: AccountDataRepository,
    incidentsRepository: IncidentsRepository,
    incidentRefresher: IncidentRefresher,
    locationsRepository: LocationsRepository,
    worksitesRepository: WorksitesRepository,
    networkMonitor: NetworkMonitor,
    languageRepository: LanguageTranslationsRepository,
    languageRefresher: LanguageRefresher,
    editableWorksiteProvider: EditableWorksiteProvider,
    translator: KeyTranslator,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val syncPusher: SyncPusher,
    resourceProvider: AndroidResourceProvider,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : EditCaseBaseViewModel(editableWorksiteProvider, translator, logger) {
    private val caseEditorArgs = CaseEditorArgs(savedStateHandle)
    private val incidentIdArg = caseEditorArgs.incidentId
    private var worksiteIdArg = caseEditorArgs.worksiteId
    val isCreateWorksite: Boolean
        get() = worksiteIdArg == null

    val headerTitle = MutableStateFlow("")

    val visibleNoteCount: Int = 2

    private val incidentFieldLookup: StateFlow<Map<String, GroupSummaryFieldLookup>>
    val workTypeGroupChildrenLookup: StateFlow<Map<String, Collection<String>>>

    val detailsFieldLookup: GroupSummaryFieldLookup?
        get() = incidentFieldLookup.value[DetailsFormGroupKey]
    val workFieldLookup: GroupSummaryFieldLookup?
        get() = incidentFieldLookup.value[WorkFormGroupKey]
    val hazardsFieldLookup: GroupSummaryFieldLookup?
        get() = incidentFieldLookup.value[HazardsFormGroupKey]
    val volunteerReportFieldLookup: GroupSummaryFieldLookup?
        get() = incidentFieldLookup.value[VolunteerReportFormGroupKey]

    val showInvalidWorksiteSave = MutableStateFlow(false)
    val invalidWorksiteInfo = mutableStateOf(InvalidWorksiteInfo())

    val editingWorksite = editableWorksiteProvider.editableWorksite

    val navigateBack = mutableStateOf(false)
    val promptUnsavedChanges = mutableStateOf(false)
    val promptCancelChanges = mutableStateOf(false)
    val isSavingWorksite = MutableStateFlow(false)

    private val dataLoader: CaseEditorDataLoader

    init {
        val headerTitleResId =
            if (isCreateWorksite) R.string.create_case
            else R.string.view_case
        headerTitle.value = resourceProvider.getString(headerTitleResId)

        editableWorksiteProvider.reset(incidentIdArg)

        dataLoader = CaseEditorDataLoader(
            isCreateWorksite,
            incidentIdArg,
            worksiteIdArg,
            accountDataRepository,
            incidentsRepository,
            incidentRefresher,
            locationsRepository,
            worksitesRepository,
            networkMonitor,
            languageRepository,
            languageRefresher,
            { key -> translate(key) },
            editableWorksiteProvider,
            viewModelScope,
            ioDispatcher,
            logger,
        )

        incidentFieldLookup = dataLoader.incidentFieldLookup
        workTypeGroupChildrenLookup = dataLoader.workTypeGroupChildrenLookup

        dataLoader.worksiteStream
            .onEach {
                it?.let { cachedWorksite ->
                    val caseNumber = cachedWorksite.worksite.caseNumber
                    // TODO Show different text if case number is empty (not yet synced)
                    headerTitle.value =
                        resourceProvider.getString(R.string.view_case_number, caseNumber)
                }
            }
            .launchIn(viewModelScope)
    }

    val uiState = dataLoader.uiState

    val isLoading = combine(
        dataLoader.isRefreshingIncident,
        dataLoader.isRefreshingWorksite,
    ) { b0, b1 -> b0 || b1 }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val hasChanges = combine(
        editingWorksite,
        uiState,
    ) { worksite, state ->
        var isChanged = false
        (state as? CaseEditorUiState.WorksiteData)?.let { data ->
            isChanged = worksite != data.worksite
        }
        isChanged
    }.stateIn(
        scope = viewModelScope,
        initialValue = false,
        started = SharingStarted.WhileSubscribed(),
    )

    val worksiteWorkTypeGroups = combine(
        editingWorksite,
        uiState,
    ) { worksite, state ->
        (state as? CaseEditorUiState.WorksiteData)?.let { stateData ->
            worksite.formData?.let { formData ->
                val incident = stateData.incident
                return@combine formData.keys
                    .asSequence()
                    .filter { incident.workTypeLookup[it] != null }
                    .mapNotNull {
                        if (workTypeGroupChildrenLookup.value.containsKey(it)) it
                        else incident.formFieldLookup[it]?.parentKey
                    }
                    .toSet()
                    .sorted()
                    .toList()
            }
        }
        emptyList()
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    private fun validate(worksite: Worksite): InvalidWorksiteInfo {
        if (worksite.name.isBlank() ||
            worksite.phone1.isBlank()
        ) {
            return InvalidWorksiteInfo(
                WorksiteSection.Property,
                R.string.incomplete_property_info,
            )
        }

        if (worksite.latitude == 0.0 ||
            worksite.longitude == 0.0 ||
            worksite.address.isBlank() ||
            worksite.postalCode.isBlank() ||
            worksite.county.isBlank() ||
            worksite.city.isBlank() ||
            worksite.state.isBlank()
        ) {
            return InvalidWorksiteInfo(
                WorksiteSection.Location,
                R.string.incomplete_location_info,
            )
        }

        val workTypeCount = worksiteWorkTypeGroups.value.size
        if (workTypeCount == 0) {
            return InvalidWorksiteInfo(
                WorksiteSection.WorkType,
                R.string.incomplete_work_type_info,
            )
        }

        return InvalidWorksiteInfo()
    }

    // TODO Refactor and add test coverage
    private fun getChangeWorkTypes(
        workTypeLookup: Map<String, String>,
        initialWorksite: Worksite,
        modifiedWorksite: Worksite,
    ): Pair<List<WorkType>, WorkType?> {
        val worksiteWorkTypes =
            initialWorksite.workTypes.associateBy(WorkType::workTypeLiteral)
        val formWorkTypes = modifiedWorksite.formData!!
            .mapNotNull { workTypeLookup[it.key] }
            .toSet()
            .map {
                val now = Clock.System.now()
                worksiteWorkTypes[it] ?: WorkType(
                    id = 0,
                    createdAt = now,
                    orgClaim = null,
                    nextRecurAt = null,
                    // TODO Does this matter
                    phase = 0,
                    recur = null,
                    statusLiteral = WorkTypeStatus.OpenUnassigned.literal,
                    workTypeLiteral = it,
                )
            }
        val initialWorkTypes = initialWorksite.workTypes.sortedBy(WorkType::workTypeLiteral)
        val modifiedWorkTypes = formWorkTypes.sortedBy(WorkType::workTypeLiteral)
        if (initialWorkTypes == modifiedWorkTypes) {
            return Pair(initialWorksite.workTypes, initialWorksite.keyWorkType)
        }

        val formWorkTypeTypes = formWorkTypes.map(WorkType::workType)
        var keyWorkType = initialWorksite.keyWorkType
        if (keyWorkType == null || !formWorkTypeTypes.contains(keyWorkType.workType)) {
            keyWorkType = formWorkTypes.toMutableList()
                .sortedBy(WorkType::workTypeLiteral)
                .firstOrNull()
        }

        return Pair(formWorkTypes, keyWorkType)
    }

    fun saveChanges(backOnSuccess: Boolean = true) {
        synchronized(isSavingWorksite) {
            if (isSavingWorksite.value) {
                return
            }
            isSavingWorksite.value = true
        }
        viewModelScope.launch(ioDispatcher) {
            try {
                val worksiteData = uiState.value as? CaseEditorUiState.WorksiteData
                val initialWorksite = worksiteData?.worksite
                    ?: return@launch

                val worksite = worksiteProvider.editableWorksite.value
                if (worksite == initialWorksite) {
                    if (backOnSuccess) {
                        navigateBack.value = true
                    }
                    return@launch
                }

                val validation = validate(worksite)
                if (validation.invalidSection != WorksiteSection.None) {
                    invalidWorksiteInfo.value = validation
                    showInvalidWorksiteSave.value = true
                    return@launch
                }

                val workTypeLookup = worksiteData.incident.workTypeLookup
                val (workTypes, primaryWorkType) = getChangeWorkTypes(
                    workTypeLookup,
                    initialWorksite,
                    worksite,
                )

                if (primaryWorkType == null) {
                    // TODO Different message? Add test coverage.
                    invalidWorksiteInfo.value = InvalidWorksiteInfo(
                        WorksiteSection.WorkType,
                        R.string.incomplete_work_type_info,
                    )
                    showInvalidWorksiteSave.value = true
                    return@launch
                }

                val updatedReportedBy =
                    if (worksite.isNew) worksiteData.orgId else worksite.reportedBy
                val clearWhat3Words = worksite.what3Words?.isNotBlank() == true &&
                        worksite.latitude != initialWorksite.latitude ||
                        worksite.longitude != initialWorksite.longitude
                val updatedWhat3Words = if (clearWhat3Words) "" else worksite.what3Words

                val updatedWorksite = worksite.copy(
                    workTypes = workTypes,
                    keyWorkType = primaryWorkType,
                    reportedBy = updatedReportedBy,
                    updatedAt = Clock.System.now(),
                    what3Words = updatedWhat3Words,
                )

                worksiteIdArg = worksiteChangeRepository.saveWorksiteChange(
                    initialWorksite,
                    updatedWorksite,
                    primaryWorkType,
                    worksiteData.orgId,
                )
                val worksiteId = worksiteIdArg!!

                dataLoader.reloadData(worksiteId)
                worksiteProvider.setEditedLocation(worksite.coordinates())

                syncPusher.appPushWorksite(worksiteId)
            } catch (e: Exception) {
                // TODO Show dialog save failed. Try again. If still fails seek help.
            } finally {
                synchronized(isSavingWorksite) {
                    isSavingWorksite.value = false
                }
            }
        }
    }

    fun abandonChanges() {
        navigateBack.value = true
    }

    /**
     * @return true if prompt is shown or false if there are no changes
     */
    private fun promptSaveChanges(): Boolean {
        if (hasChanges.value) {
            promptUnsavedChanges.value = true
            return true
        }
        return false
    }

    private fun onBackNavigate(): Boolean {
        if (isSavingWorksite.value) {
            return false
        }

        return !promptSaveChanges()
    }

    override fun onSystemBack() = onBackNavigate()

    override fun onNavigateBack() = onBackNavigate()

    override fun onNavigateCancel(): Boolean {
        if (isSavingWorksite.value) {
            return false
        }

        if (hasChanges.value) {
            promptCancelChanges.value = true
            return false
        }
        return true
    }
}

sealed interface CaseEditorUiState {
    object Loading : CaseEditorUiState

    data class WorksiteData(
        val orgId: Long,
        val isEditable: Boolean,
        val worksite: Worksite,
        val incident: Incident,
        val localWorksite: LocalWorksite?,
        val networkWorksiteSync: Pair<Long, NetworkWorksiteFull>?,
    ) : CaseEditorUiState {
        val isLocalModified = localWorksite?.localChanges?.isLocalModified ?: false
    }

    data class Error(
        val errorResId: Int = 0,
        val errorMessage: String = "",
    ) : CaseEditorUiState
}

data class GroupSummaryFieldLookup(
    val fieldMap: Map<String, String>,
    val optionTranslations: Map<String, String>,
)

data class InvalidWorksiteInfo(
    val invalidSection: WorksiteSection = WorksiteSection.None,
    @StringRes val messageResId: Int = 0,
)

enum class WorksiteSection {
    None,
    Property,
    Location,
    WorkType,
}