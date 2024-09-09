package com.crisiscleanup.feature.team

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.crisiscleanup.feature.team.navigation.TeamEditorArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CreateEditTeamViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val teamEditorArgs = TeamEditorArgs(savedStateHandle)
    private val teamIdArg = teamEditorArgs.teamId
    val startingEditorStep = teamEditorArgs.editorStep
}
