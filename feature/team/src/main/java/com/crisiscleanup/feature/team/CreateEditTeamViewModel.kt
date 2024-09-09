package com.crisiscleanup.feature.team

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.ReplaySubscribed3
import com.crisiscleanup.feature.team.model.TeamEditorStep
import com.crisiscleanup.feature.team.model.stepFromLiteral
import com.crisiscleanup.feature.team.navigation.TeamEditorArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CreateEditTeamViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val translator: KeyTranslator,
) : ViewModel(), KeyTranslator {
    private val teamEditorArgs = TeamEditorArgs(savedStateHandle)
    private val teamIdArg = teamEditorArgs.teamId
    private val startingEditorStep = stepFromLiteral(teamEditorArgs.editorStep)

    private val stepTabOrder = MutableStateFlow(
        listOf(
            TeamEditorStep.Name,
            TeamEditorStep.Members,
            TeamEditorStep.Cases,
            TeamEditorStep.Equipment,
            TeamEditorStep.Review,
        ),
    )

    val stepTabState = stepTabOrder.map { order ->
        val titles = order.mapIndexed { index, step ->
            "${index + 1}. ${translate(step.translateKey)}"
        }
        val startingIndex = order.indexOf(startingEditorStep)
            .coerceIn(0, titles.size - 1)
        CreateEditTeamTabState(titles, startingIndex)
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = CreateEditTeamTabState(),
            started = ReplaySubscribed3,
        )

    // TODO After team is loaded
    var screenTitle by mutableStateOf("")
        private set
    var screenSubTitle by mutableStateOf("")
        private set


    // KeyTranslator

    override val translationCount = translator.translationCount

    override fun translate(phraseKey: String) = translator.translate(phraseKey) ?: phraseKey
}

data class CreateEditTeamTabState(
    val titles: List<String> = emptyList(),
    val startingIndex: Int = 0,
)