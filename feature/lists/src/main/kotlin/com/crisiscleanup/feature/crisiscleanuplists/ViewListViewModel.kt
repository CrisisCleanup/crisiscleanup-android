package com.crisiscleanup.feature.crisiscleanuplists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.crisiscleanup.feature.crisiscleanuplists.navigation.ViewListArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ViewListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val viewListArgs = ViewListArgs(savedStateHandle)

    // TODO Make private
    val listId = viewListArgs.listId

    // TODO Load list data or show error
}