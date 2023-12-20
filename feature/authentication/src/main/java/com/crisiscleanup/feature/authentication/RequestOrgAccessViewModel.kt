package com.crisiscleanup.feature.authentication

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.crisiscleanup.feature.authentication.navigation.RequestOrgAccessArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RequestOrgAccessViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val editorArgs = RequestOrgAccessArgs(savedStateHandle)

    val inviteCode = editorArgs.inviteCode
}