package com.crisiscleanup.feature.crisiscleanuplists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.ListsRepository
import com.crisiscleanup.core.model.data.CrisisCleanupList
import com.crisiscleanup.core.model.data.EmptyList
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
    translator: KeyResourceTranslator,
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

    init {
        viewModelScope.launch(ioDispatcher) {
            listsRepository.refreshList(listId)
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
