package com.crisiscleanup.feature.crisiscleanuplists

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifierNone
import com.crisiscleanup.core.data.repository.ListsRepository
import com.crisiscleanup.core.model.data.CrisisCleanupList
import com.crisiscleanup.core.model.data.EmptyList
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.crisiscleanuplists.navigation.ViewListArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    listsRepository: ListsRepository,
    private val translator: KeyResourceTranslator,
    @Logger(CrisisCleanupLoggers.Lists) private val logger: AppLogger,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val viewListArgs = ViewListArgs(savedStateHandle)

    private val listId = viewListArgs.listId

    val viewState = listsRepository.streamList(listId)
        .map { list ->
            if (list == EmptyList) {
                val listNotFound =
                    translator("~~List was not found. It is likely deleted.")
                return@map ViewListViewState.Error(listNotFound)
            }

            val lookup = listsRepository.getListObjectData(list)
            val objectData = list.objectIds.map { id ->
                lookup[id]
            }
            ViewListViewState.Success(list, objectData)
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = ViewListViewState.Loading,
            started = SharingStarted.WhileSubscribed(3_000),
        )

    val screenTitle = viewState.map {
        (it as? ViewListViewState.Success)?.list?.let { list ->
            return@map list.name
        }

        translator("~~List")
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(3_000),
        )

    var isConfirmingOpenWorksite by mutableStateOf(false)
        private set
    var openWorksiteId by mutableStateOf(ExistingWorksiteIdentifierNone)
    var openWorksiteError by mutableStateOf("")

    init {
        viewModelScope.launch(ioDispatcher) {
            listsRepository.refreshList(listId)
        }
    }

    fun onOpenWorksite(worksite: Worksite) {
        if (worksite == EmptyWorksite) {
            return
        }

        if (isConfirmingOpenWorksite) {
            return
        }
        isConfirmingOpenWorksite = true

        viewModelScope.launch(ioDispatcher) {
            try {
                (viewState.value as? ViewListViewState.Success)?.list.let { list ->
                    if (list?.incident?.id == worksite.incidentId) {
                        openWorksiteId = ExistingWorksiteIdentifier(
                            incidentId = worksite.incidentId,
                            worksiteId = worksite.id,
                        )
                    } else {
                        openWorksiteError =
                            translator("~~Case {case_number} does not belong in Incident {incident_name}")
                                .replace("{case_number}", worksite.caseNumber)
                                .replace("{incident_name}", list?.incident?.shortName ?: "")
                                .trim()
                    }
                }
            } catch (e: Exception) {
                logger.logException(e)
            } finally {
                isConfirmingOpenWorksite = false
            }
        }
    }
}

sealed interface ViewListViewState {
    data object Loading : ViewListViewState

    data class Success(
        val list: CrisisCleanupList,
        val objectData: List<Any?>,
    ) : ViewListViewState

    data class Error(
        val message: String,
    ) : ViewListViewState
}
