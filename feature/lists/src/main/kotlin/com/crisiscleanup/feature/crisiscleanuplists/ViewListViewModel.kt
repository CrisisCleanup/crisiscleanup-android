package com.crisiscleanup.feature.crisiscleanuplists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.data.repository.ListsRepository
import com.crisiscleanup.core.model.data.CrisisCleanupList
import com.crisiscleanup.core.model.data.EmptyList
import com.crisiscleanup.feature.crisiscleanuplists.navigation.ViewListArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ViewListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    listsRepository: ListsRepository,
    translator: KeyResourceTranslator,
) : ViewModel() {
    private val viewListArgs = ViewListArgs(savedStateHandle)

    private val listId = viewListArgs.listId

    val viewState = listsRepository.streamList(listId)
        .map { list ->
            if (list == EmptyList) {
                val listNotFound =
                    translator("~~List was not found. Go back and try selecting the list again.")
                return@map ViewListViewState.Error(listNotFound)
            }

            ViewListViewState.Success(list)
        }
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
        // TODO Load list data or show error
    }
}

sealed interface ViewListViewState {
    data object Loading : ViewListViewState

    data class Success(
        val list: CrisisCleanupList,
    ) : ViewListViewState

    data class Error(
        val message: String,
    ) : ViewListViewState
}