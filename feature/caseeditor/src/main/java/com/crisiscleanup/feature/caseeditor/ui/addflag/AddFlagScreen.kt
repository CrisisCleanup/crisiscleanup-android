package com.crisiscleanup.feature.caseeditor.ui.addflag

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifierNone
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.AnimatedBusyIndicator
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.TopAppBarCancelAction
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemBottomPadding
import com.crisiscleanup.core.designsystem.theme.listItemDropdownMenuOffset
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.optionItemHeight
import com.crisiscleanup.core.designsystem.theme.optionItemPadding
import com.crisiscleanup.core.model.data.OrganizationIdName
import com.crisiscleanup.core.model.data.WorksiteFlagType
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.rememberIsKeyboardOpen
import com.crisiscleanup.feature.caseeditor.CaseAddFlagViewModel
import com.crisiscleanup.feature.caseeditor.util.TwoActionBar

@Composable
internal fun CaseEditAddFlagRoute(
    onBack: () -> Unit = {},
    viewModel: CaseAddFlagViewModel = hiltViewModel(),
    rerouteIncidentChange: (ExistingWorksiteIdentifier) -> Unit = {},
) {
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    val incidentWorksiteChange by viewModel.incidentWorksiteChange.collectAsStateWithLifecycle()
    if (isSaved) {
        onBack()
    } else if (incidentWorksiteChange != ExistingWorksiteIdentifierNone) {
        rerouteIncidentChange(incidentWorksiteChange)
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

    var flagFlow by remember { mutableStateOf<WorksiteFlagType?>(null) }
    val updateFlagFlow =
        remember(viewModel) { { selected: WorksiteFlagType -> flagFlow = selected } }
    val flagFlowOptions by viewModel.flagFlows.collectAsStateWithLifecycle()

    val setWrongLocationFlag =
        remember(viewModel) { { viewModel.onAddFlag(WorksiteFlagType.WrongLocation) } }

    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val isEditable by viewModel.isEditable.collectAsStateWithLifecycle()

    CompositionLocalProvider(
        LocalAppTranslator provides translator,
    ) {
        Column {
            TopAppBarCancelAction(
                title = viewModel.screenTitle,
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
                WorksiteFlagType.HighPriority -> HighPriorityFlagView(
                    onBack = onBack,
                    isEditable = isEditable,
                )

                WorksiteFlagType.UpsetClient -> UpsetClientFlagView(
                    onBack = onBack,
                    isEditable = isEditable,
                )

                WorksiteFlagType.MarkForDeletion -> GeneralFlagView(
                    WorksiteFlagType.MarkForDeletion,
                    onBack = onBack,
                    isEditable = isEditable,
                )

                WorksiteFlagType.ReportAbuse -> ReportAbuseFlagView(
                    onBack = onBack,
                    isEditable = isEditable,
                )

                WorksiteFlagType.Duplicate -> GeneralFlagView(
                    WorksiteFlagType.Duplicate,
                    onBack = onBack,
                    isEditable = isEditable,
                )

                WorksiteFlagType.WrongLocation -> WrongLocationFlagView(
                    onBack = onBack,
                    isEditable = isEditable,
                    setWrongLocationFlag = setWrongLocationFlag,
                )

                WorksiteFlagType.WrongIncident -> WrongIncidentFlagView(
                    onBack = onBack,
                    isEditable = isEditable,
                )

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
    val translator = LocalAppTranslator.current

    var contentSize by remember { mutableStateOf(Size.Zero) }

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
                    contentSize = it.size.toSize()
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
            modifier = Modifier.width(with(LocalDensity.current) { contentSize.width.toDp() }),
            expanded = showDropdown && isEditable,
            onDismissRequest = { showDropdown = false },
        ) {
            for (option in options) {
                key(option.literal) {
                    DropdownMenuItem(
                        modifier = Modifier.optionItemHeight(),
                        text = {
                            Text(
                                translator(option.literal),
                                style = LocalFontStyles.current.header4,
                            )
                        },
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
internal fun OrganizationsSearch(
    orgQuery: String,
    onQueryChange: (String) -> Unit,
    orgSuggestions: List<OrganizationIdName>,
    onOrgSelected: (OrganizationIdName) -> Unit,
    rememberKey: Any,
    isEditable: Boolean = false,
) {
    val translator = LocalAppTranslator.current
    val closeKeyboard = rememberCloseKeyboard(rememberKey)

    var dismissSuggestionsQuery by remember { mutableStateOf("") }

    var contentSize by remember { mutableStateOf(Size.Zero) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedClearableTextField(
            modifier = Modifier
                .fillMaxWidth()
                .listItemHorizontalPadding()
                .listItemBottomPadding()
                .onGloballyPositioned {
                    contentSize = it.size.toSize()
                },
            labelResId = 0,
            label = translator("profileOrg.organization_name"),
            value = orgQuery,
            onValueChange = {
                dismissSuggestionsQuery = ""
                onQueryChange(it)
            },
            keyboardType = KeyboardType.Text,
            keyboardCapitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
            isError = false,
            hasFocus = false,
            onEnter = closeKeyboard,
            enabled = isEditable,
        )

        var selectedOptionQuery by remember { mutableStateOf("") }
        val dismissDropdown = { dismissSuggestionsQuery = orgQuery }
        val showDropdown by remember(
            orgSuggestions,
            dismissSuggestionsQuery,
            selectedOptionQuery,
            orgQuery,
        ) {
            derivedStateOf {
                orgSuggestions.isNotEmpty() &&
                    dismissSuggestionsQuery != orgQuery &&
                    selectedOptionQuery != orgQuery
            }
        }
        DropdownMenu(
            modifier = Modifier
                .width(with(LocalDensity.current) { contentSize.width.toDp() })
                // TODO Use inner window height - approximate keyboard height
                .heightIn(max = 300.dp),
            expanded = showDropdown,
            onDismissRequest = dismissDropdown,
            offset = listItemDropdownMenuOffset,
            properties = PopupProperties(focusable = false),
        ) {
            BackHandler(showDropdown) {
                dismissDropdown()
            }
            orgSuggestions.forEach { organization ->
                DropdownMenuItem(
                    text = {
                        Text(
                            organization.name,
                            Modifier.optionItemPadding(),
                            style = LocalFontStyles.current.header4,
                        )
                    },
                    onClick = {
                        selectedOptionQuery = organization.name
                        onQueryChange(organization.name)
                        onOrgSelected(organization)
                    },
                )
            }
        }
    }
}

@Composable
internal fun AddFlagSaveActionBar(
    onSave: () -> Unit = {},
    onCancel: () -> Unit = {},
    enabled: Boolean = false,
    enableSave: Boolean = true,
    isBusy: Boolean = false,
) {
    val isKeyboardOpen = rememberIsKeyboardOpen()
    if (!isKeyboardOpen) {
        TwoActionBar(
            onPositiveAction = onSave,
            onCancel = onCancel,
            enabled = enabled,
            enablePositive = enableSave,
            isBusy = isBusy,
        )
    }
}

@Composable
private fun ColumnScope.GeneralFlagView(
    flagType: WorksiteFlagType,
    viewModel: CaseAddFlagViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    isEditable: Boolean = false,
) {
    Spacer(Modifier.weight(1f))

    AddFlagSaveActionBar(
        onSave = { viewModel.onAddFlag(flagType) },
        onCancel = onBack,
        enabled = isEditable,
    )
}
