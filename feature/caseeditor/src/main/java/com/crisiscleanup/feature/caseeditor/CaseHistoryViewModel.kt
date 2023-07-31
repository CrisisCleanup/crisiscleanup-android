package com.crisiscleanup.feature.caseeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.CaseHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaseHistoryViewModel @Inject constructor(
    editableWorksiteProvider: EditableWorksiteProvider,
    caseHistoryRepository: CaseHistoryRepository,
    val translator: KeyResourceTranslator,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val worksite = editableWorksiteProvider.editableWorksite.value
    private val worksiteId = worksite.id

    val isLoadingCaseHistory = caseHistoryRepository.loadingWorksiteId.map {
        it == worksiteId
    }

    val screenTitle = "${translator("actions.history")} (${worksite.caseNumber})"

    val historyEvents = flowOf(worksiteId)
        .flatMapLatest {
            caseHistoryRepository.streamEvents(it)
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    val isHistoryLoaded = historyEvents.map { true }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    init {
        viewModelScope.launch(ioDispatcher) {
            caseHistoryRepository.refreshEvents(worksiteId)
        }
    }
}