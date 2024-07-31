package com.crisiscleanup.feature.team

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.crisiscleanup.feature.team.navigation.ViewTeamArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ViewTeamViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val viewTeamArgs = ViewTeamArgs(savedStateHandle)
    val teamId = viewTeamArgs.teamId
}
