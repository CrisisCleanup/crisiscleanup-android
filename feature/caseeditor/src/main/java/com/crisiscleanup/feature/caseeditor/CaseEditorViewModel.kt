package com.crisiscleanup.feature.caseeditor

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.appheader.AppHeaderUiState
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
    private val appHeaderUiState: AppHeaderUiState,
    private val resourceProvider: AndroidResourceProvider,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val caseEditorArgs = CaseEditorArgs(savedStateHandle)

    private val _uiState = MutableStateFlow<CaseEditorUiState>(CaseEditorUiState.Loading)
    val uiState: StateFlow<CaseEditorUiState> = _uiState

    private val _isReadOnly = MutableStateFlow(true)
    val isReadOnly: StateFlow<Boolean> = _isReadOnly

    private val _isRefreshingWorksite = MutableStateFlow(false)
    val isLoadingWorksite: StateFlow<Boolean> = _isRefreshingWorksite

    private var _formFields = mutableStateOf(emptyList<FormFieldNode>())
    private val formFields: State<List<FormFieldNode>> = _formFields

    init {
        val headerTitleResId =
            if (caseEditorArgs.worksiteId == null) R.string.create_case else R.string.edit_case
        appHeaderUiState.pushTitle(resourceProvider.getString(headerTitleResId))

        viewModelScope.launch(ioDispatcher) {
            val incidentId = caseEditorArgs.incidentId

            var worksite: Worksite? = null
            var localWorksite: LocalWorksite? = null
            var networkWorksite: Worksite? = null

            val worksiteIdArg = caseEditorArgs.worksiteId

            // TODO Sync incidents, worksite, and languages in parallel and process all data at end

            // Load network incident optimistically
            try {
                incidentsRepository.pullIncident(incidentId)
            } catch (e: Exception) {
                logger.logException(e)
            }

            // Load local incident or fail
            val incident = incidentsRepository.getIncident(incidentId, true)
            if (incident == null) {
                logger.logException(Exception("Incident $incidentId not found when editing worksite $worksiteIdArg"))
                _uiState.value = CaseEditorUiState.Error(R.string.incident_issue_try_again)
                return@launch
            }

            _formFields.value = FormFieldNode.buildTree(incident.formFields, languageRepository)

            try {
                // TODO Track language sync attempts and skip if last attempt is less than x hours ago where x hours is parameterized?
                languageRepository.loadLanguages()
            } catch (e: Exception) {
                logger.logException(e)
            }

            worksiteIdArg?.let { worksiteId ->
                localWorksite = worksitesRepository.getLocalWorksite(worksiteId)
                val cachedWorksite = localWorksite!!
                val isLocalModified = cachedWorksite.localChanges.isLocalModified

                val caseNumber = cachedWorksite.worksite.caseNumber
                val headerTitle =
                    resourceProvider.getString(R.string.edit_case_case_number, caseNumber)
                appHeaderUiState.setTitle(headerTitle)

                worksite = cachedWorksite.worksite.copy()
                _uiState.value = CaseEditorUiState.WorksiteData(
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
                        networkWorksite = worksitesRepository.refreshWorksite(incidentId, networkId)
                        if (!isLocalModified) {
                            localWorksite = worksitesRepository.getLocalWorksite(worksiteId)
                            worksite = localWorksite!!.worksite.copy()
                        }
                    } catch (e: Exception) {
                        // TODO This is going to be difficult. Plenty of state for possible change...
                        //      Show error message of some sort

                        logger.logException(e)
                    } finally {
                        _isRefreshingWorksite.value = false
                    }
                }
            }

            _isReadOnly.value = false

            _uiState.value = CaseEditorUiState.WorksiteData(
                worksite ?: EmptyWorksite,
                incident,
                localWorksite,
                networkWorksite,
            )
        }
    }
}

sealed interface CaseEditorUiState {
    object Loading : CaseEditorUiState
    data class WorksiteData(
        val worksite: Worksite,
        val incident: Incident?,
        val localWorksite: LocalWorksite?,
        val networkWorksite: Worksite?,
    ) : CaseEditorUiState {
        val isLocalModified = localWorksite?.localChanges?.isLocalModified ?: false
    }

    data class Error(
        val errorResId: Int = 0,
        val errorMessage: String = "",
    ) : CaseEditorUiState
}