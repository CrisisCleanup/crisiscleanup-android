package com.crisiscleanup.feature.mediamanage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.appnav.ViewImageArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ViewImageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val caseEditorArgs = ViewImageArgs(savedStateHandle)
    private val imageId = caseEditorArgs.imageId
    val imageUrl = caseEditorArgs.imageUrl
    private val isNetworkImage = caseEditorArgs.isNetworkImage

}