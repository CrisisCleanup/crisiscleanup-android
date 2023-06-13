package com.crisiscleanup.feature.caseeditor.ui.addflag

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.theme.listItemDropdownMenuOffset
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.optionItemPadding
import com.crisiscleanup.core.model.data.IncidentIdNameType
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.CaseAddFlagViewModel

@Composable
internal fun ColumnScope.WrongIncidentFlagView(
    viewModel: CaseAddFlagViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    isEditable: Boolean = false,
) {
    val translator = LocalAppTranslator.current.translator

    val closeKeyboard = rememberCloseKeyboard(viewModel)

    val incidentQuery by viewModel.incidentQ.collectAsStateWithLifecycle()
    val onIncidentQueryChange = remember(viewModel) {
        { q: String -> viewModel.onIncidentQueryChange(q) }
    }
    var selectedIncident by remember { mutableStateOf<IncidentIdNameType?>(null) }
    val incidentSuggestions by viewModel.incidentResults.collectAsStateWithLifecycle()

    var isCorrectIncidentListed by remember { mutableStateOf(true) }

    LazyColumn(
        Modifier
            .scrollFlingListener(closeKeyboard)
            .weight(1f)
            .fillMaxWidth()
    ) {
        labelTextItem(translator("flag.choose_correct_incident"))

        item {
            Box(Modifier.fillMaxWidth()) {
                var contentWidth by remember { mutableStateOf(Size.Zero) }

                var dismissSuggestionsQuery by remember { mutableStateOf("") }
                OutlinedClearableTextField(
                    modifier = listItemModifier.onGloballyPositioned {
                        contentWidth = it.size.toSize()
                    },
                    labelResId = 0,
                    label = translator("casesVue.incident"),
                    value = incidentQuery,
                    onValueChange = {
                        dismissSuggestionsQuery = ""
                        onIncidentQueryChange(it)
                    },
                    keyboardType = KeyboardType.Text,
                    keyboardCapitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                    isError = false,
                    hasFocus = true,
                    onEnter = closeKeyboard,
                    enabled = isEditable && isCorrectIncidentListed,
                )

                var selectedOptionQuery by remember { mutableStateOf("") }
                val dismissDropdown =
                    remember(viewModel) { { dismissSuggestionsQuery = incidentQuery } }
                val showDropdown by remember(
                    incidentSuggestions,
                    dismissSuggestionsQuery,
                    selectedOptionQuery,
                    incidentQuery,
                    isCorrectIncidentListed,
                ) {
                    derivedStateOf {
                        incidentSuggestions.isNotEmpty() &&
                                dismissSuggestionsQuery != incidentQuery &&
                                selectedOptionQuery != incidentQuery &&
                                isCorrectIncidentListed
                    }
                }
                DropdownMenu(
                    modifier = Modifier
                        .width(with(LocalDensity.current) { contentWidth.width.toDp() })
                        // TODO Use inner window height - approximate keyboard height
                        .heightIn(max = 300.dp),
                    expanded = showDropdown,
                    onDismissRequest = dismissDropdown,
                    offset = listItemDropdownMenuOffset,
                    properties = PopupProperties(focusable = false)
                ) {
                    BackHandler {
                        dismissDropdown()
                    }
                    incidentSuggestions.forEach { incident ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    incident.name,
                                    Modifier.optionItemPadding(),
                                )
                            },
                            onClick = {
                                selectedOptionQuery = incident.name
                                onIncidentQueryChange(incident.name)
                                selectedIncident = incident
                            },
                        )
                    }
                }
            }
        }

        item {
            val updateIncidentQuery = remember(viewModel) {
                { b: Boolean ->
                    isCorrectIncidentListed = b
                    if (!isCorrectIncidentListed) {
                        onIncidentQueryChange("")
                    }
                }
            }
            CrisisCleanupTextCheckbox(
                checked = !isCorrectIncidentListed,
                onToggle = { updateIncidentQuery(!isCorrectIncidentListed) },
                onCheckChange = { updateIncidentQuery(it) },
                text = translator("flag.incident_not_listed"),
            )
        }
    }

    val onSave = remember(viewModel) {
        {
            viewModel.onWrongIncident(isCorrectIncidentListed, selectedIncident)
        }
    }
    AddFlagSaveActionBar(
        onSave = onSave,
        onCancel = onBack,
        enabled = isEditable,
    )
}