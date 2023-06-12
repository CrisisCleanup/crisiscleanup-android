package com.crisiscleanup.feature.caseeditor.ui.addflag

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.designsystem.AppTranslator
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.AnimatedBusyIndicator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.TopAppBarCancelAction
import com.crisiscleanup.core.designsystem.component.cancelButtonColors
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.optionItemHeight
import com.crisiscleanup.core.designsystem.theme.textBoxHeight
import com.crisiscleanup.core.model.data.WorksiteFlagType
import com.crisiscleanup.feature.caseeditor.CaseAddFlagViewModel
import com.crisiscleanup.feature.caseeditor.R

@Composable
internal fun CaseEditAddFlagRoute(
    onBack: () -> Unit = {},
    viewModel: CaseAddFlagViewModel = hiltViewModel(),
) {
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    if (isSaved) {
        onBack()
    } else {
        CaseEditAddFlagScreen(onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaseEditAddFlagScreen(
    onBack: () -> Unit = {},
    viewModel: CaseAddFlagViewModel = hiltViewModel(),
) {
    val translator = viewModel.translator
    val appTranslator = remember(viewModel) {
        AppTranslator(translator = translator)
    }

    var flagFlow by remember { mutableStateOf<WorksiteFlagType?>(null) }
    val updateFlagFlow =
        remember(viewModel) { { selected: WorksiteFlagType -> flagFlow = selected } }
    val flagFlowOptions by viewModel.flagFlows.collectAsStateWithLifecycle()

    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val isEditable by viewModel.isEditable.collectAsStateWithLifecycle()

    CompositionLocalProvider(
        LocalAppTranslator provides appTranslator,
    ) {
        Column {
            TopAppBarCancelAction(
                title = translator("caseForm.add_flag", R.string.add_flag),
                onAction = onBack,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .listItemHorizontalPadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    // TODO This should be duplicated and use a different key
                    translator("events.object_flag"),
                    style = MaterialTheme.typography.bodyLarge,
                )
                FlagsDropdown(
                    flagFlow,
                    flagFlowOptions,
                    Modifier.listItemPadding(),
                    onSelectedFlagFlow = updateFlagFlow,
                    isEditable = isEditable,
                    isLoading = isSaving,
                )
            }

            when (flagFlow) {
                WorksiteFlagType.HighPriority -> HighPriorityView(
                    onBack = onBack,
                    isEditable = isEditable,
                )

                WorksiteFlagType.UpsetClient -> UpsetClientView(translator = translator)
                WorksiteFlagType.MarkForDeletion -> MarkForDeletionView(translator = translator)
                WorksiteFlagType.ReportAbuse -> ReportAbuseView(translator = translator)
                WorksiteFlagType.Duplicate -> DuplicateView(translator = translator)
                WorksiteFlagType.WrongLocation -> WrongLocationView(translator = translator)
                WorksiteFlagType.WrongIncident -> WrongIncidentView(translator = translator)
                else -> {}
            }
        }
    }
}

@Composable
private fun FlagsDropdown(
    selectedFlagFlow: WorksiteFlagType?,
    options: Collection<WorksiteFlagType>,
    modifier: Modifier = Modifier,
    onSelectedFlagFlow: (WorksiteFlagType) -> Unit = {},
    isEditable: Boolean = false,
    isLoading: Boolean = false,
) {
    val translator = LocalAppTranslator.current.translator

    var contentWidth by remember { mutableStateOf(Size.Zero) }

    val selectedText = translator(selectedFlagFlow?.literal ?: "flag.choose_problem")
    Box(modifier) {
        var showDropdown by remember { mutableStateOf(false) }
        Row(
            Modifier
                .clickable(
                    onClick = { showDropdown = !showDropdown },
                    enabled = isEditable,
                )
                .onGloballyPositioned {
                    contentWidth = it.size.toSize()
                }
                .then(listItemModifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // TODO Change color when not editable
            Text(
                selectedText,
                style = MaterialTheme.typography.bodyLarge,
            )

            Icon(
                imageVector = CrisisCleanupIcons.ArrowDropDown,
                contentDescription = translator("flag.choose_problem"),
            )
            AnimatedBusyIndicator(isLoading, padding = 8.dp)
        }

        DropdownMenu(
            modifier = Modifier
                .width(with(LocalDensity.current) { contentWidth.width.toDp() }),
            expanded = showDropdown && isEditable,
            onDismissRequest = { showDropdown = false },
        ) {
            for (option in options) {
                key(option.literal) {
                    DropdownMenuItem(
                        modifier = Modifier.optionItemHeight(),
                        text = { Text(translator(option.literal)) },
                        onClick = {
                            onSelectedFlagFlow(option)
                            showDropdown = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun TextArea(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit = {},
    onDone: () -> Unit = {},
    hasFocus: Boolean = false,
    isEditable: Boolean = false,
) {
    val focusRequester = FocusRequester()

    val keyboardOptions = KeyboardOptions(
        imeAction = ImeAction.Done,
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Sentences,
    )
    val keyboardActions = KeyboardActions(
        onDone = { onDone() },
    )
    OutlinedTextField(
        text,
        { value: String -> onTextChange(value) },
        modifier = modifier
            .textBoxHeight()
            .focusRequester(focusRequester),
        label = label,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        enabled = isEditable,
    )
    if (hasFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

internal fun LazyListScope.textItem(
    text: String,
    isBold: Boolean = false,
) {
    item(contentType = "item-text") {
        Text(
            text,
            listItemModifier,
            fontWeight = if (isBold) FontWeight.Bold else null,
        )
    }
}

@Composable
internal fun AddFlagSaveActionBar(
    onSave: () -> Unit = {},
    onCancel: () -> Unit = {},
    isEditable: Boolean = false,
) {
    val translator = LocalAppTranslator.current.translator
    Row(
        modifier = Modifier
            // TODO Common dimensions
            .padding(16.dp),
        horizontalArrangement = listItemSpacedBy,
    ) {
        BusyButton(
            Modifier.weight(1f),
            text = translator("actions.cancel"),
            enabled = isEditable,
            onClick = onCancel,
            colors = cancelButtonColors(),
        )
        BusyButton(
            Modifier.weight(1f),
            text = translator("actions.save"),
            enabled = isEditable,
            onClick = onSave,
        )
    }
}

@Composable
private fun UpsetClientView(
    viewModel: CaseAddFlagViewModel = hiltViewModel(),
    translator: KeyResourceTranslator,
) {
    Text("Calm em down")
}

@Composable
private fun MarkForDeletionView(
    viewModel: CaseAddFlagViewModel = hiltViewModel(),
    translator: KeyResourceTranslator,
) {
    Text("Are you sure?")
}

@Composable
private fun ReportAbuseView(
    viewModel: CaseAddFlagViewModel = hiltViewModel(),
    translator: KeyResourceTranslator,
) {
    Text("Fill out this form")
}

@Composable
private fun DuplicateView(
    viewModel: CaseAddFlagViewModel = hiltViewModel(),
    translator: KeyResourceTranslator,
) {
    Text("Another one?")
}

@Composable
private fun WrongLocationView(
    viewModel: CaseAddFlagViewModel = hiltViewModel(),
    translator: KeyResourceTranslator,
) {
    Text("Misplaced!")
}

@Composable
private fun WrongIncidentView(
    viewModel: CaseAddFlagViewModel = hiltViewModel(),
    translator: KeyResourceTranslator,
) {
    Text("Move it")
}
