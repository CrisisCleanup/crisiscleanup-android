package com.crisiscleanup.feature.caseeditor

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LanguageTranslationsRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.model.data.*
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import com.crisiscleanup.feature.caseeditor.model.FormFieldNode
import com.crisiscleanup.feature.caseeditor.navigation.CaseEditorArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaseEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    incidentsRepository: IncidentsRepository,
    worksitesRepository: WorksitesRepository,
    languageRepository: LanguageTranslationsRepository,
    private val editableWorksiteProvider: EditableWorksiteProvider,
    private val resourceProvider: AndroidResourceProvider,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val caseEditorArgs = CaseEditorArgs(savedStateHandle)
    val isCreateWorksite = caseEditorArgs.worksiteId == null

    val uiState = MutableStateFlow<CaseEditorUiState>(CaseEditorUiState.Loading)

    val isReadOnly = MutableStateFlow(true)

    val headerTitle = MutableStateFlow("")

    private val _isRefreshingWorksite = MutableStateFlow(false)
    val isLoadingWorksite: StateFlow<Boolean> = _isRefreshingWorksite

    val editableWorksite: Worksite
        get() = editableWorksiteProvider.editableWorksite

    val navigateBack = mutableStateOf(false)

    init {
        val headerTitleResId =
            if (caseEditorArgs.worksiteId == null) R.string.create_case else R.string.edit_case
        headerTitle.value = resourceProvider.getString(headerTitleResId)

        viewModelScope.launch(ioDispatcher) {
            var worksite: Worksite? = null
            var localWorksite: LocalWorksite? = null
            var networkWorksiteSync: Pair<Long, NetworkWorksiteFull>? = null

            val worksiteIdArg = caseEditorArgs.worksiteId

            // TODO Sync incidents, worksite, and languages in parallel and process all data at end

            val incidentId = caseEditorArgs.incidentId
            try {
                incidentsRepository.pullIncident(incidentId)
            } catch (e: Exception) {
                logger.logException(e)
            }
            val incident = incidentsRepository.getIncident(incidentId, true)
            if (incident?.formFields?.isEmpty() != false) {
                logger.logException(Exception("Incident $incidentId not found when editing worksite $worksiteIdArg"))
                uiState.value = CaseEditorUiState.Error(R.string.incident_issue_try_again)
                return@launch
            }

            try {
                languageRepository.loadLanguages()
            } catch (e: Exception) {
                logger.logException(e)
            }

            worksiteIdArg?.let { worksiteId ->
                localWorksite = worksitesRepository.getLocalWorksite(worksiteId)
                val cachedWorksite = localWorksite!!
                val isLocalModified = cachedWorksite.localChanges.isLocalModified

                val caseNumber = cachedWorksite.worksite.caseNumber
                headerTitle.value =
                    resourceProvider.getString(R.string.edit_case_case_number, caseNumber)

                worksite = cachedWorksite.worksite.copy()
                uiState.value = CaseEditorUiState.WorksiteData(
                    worksite!!,
                    incident,
                    cachedWorksite,
                    null,
                )

                val networkId = cachedWorksite.worksite.networkId
                val isNetworkedWorksite = networkId > 0
                if (isNetworkedWorksite) {
                    _isRefreshingWorksite.value = true
                    try {
                        networkWorksiteSync =
                            worksitesRepository.syncWorksite(incidentId, networkId)
                        // TODO How to reliably resolve changes between local and network if exists? Ask to overwrite from network or continue with local? Which may overwrite network when pushed?
                        if (!isLocalModified) {
                            localWorksite = worksitesRepository.getLocalWorksite(worksiteId)
                            worksite = localWorksite!!.worksite.copy()
                        }

                        isReadOnly.value = false
                    } catch (e: Exception) {
                        // TODO This is going to be difficult. Plenty of state for possible change...
                        //      Show error message of some sort

                        logger.logException(e)
                    } finally {
                        _isRefreshingWorksite.value = false
                    }
                }
            }

            if (isCreateWorksite) {
                isReadOnly.value = false
            }

            val initialWorksite = worksite ?: EmptyWorksite.copy(
                autoContactFrequencyT = AutoContactFrequency.Often.literal,
            )
            // TODO Atomic set just in case
            with(editableWorksiteProvider) {
                editableWorksite = initialWorksite.copy()
                formFields = FormFieldNode.buildTree(incident.formFields, languageRepository)
            }

            logger.logDebug("Loaded for editing", editableWorksiteProvider.editableWorksite.id)

            uiState.value = CaseEditorUiState.WorksiteData(
                initialWorksite,
                incident,
                localWorksite,
                networkWorksiteSync,
            )
        }
    }

    fun onNavigateBack(): Boolean {
        return true
    }

    fun onNavigateCancel(): Boolean {
        return true
    }
}

sealed interface CaseEditorUiState {
    object Loading : CaseEditorUiState

    data class WorksiteData(
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