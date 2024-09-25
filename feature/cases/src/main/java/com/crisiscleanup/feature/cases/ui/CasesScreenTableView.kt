package com.crisiscleanup.feature.cases.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.common.ParsedPhoneNumber
import com.crisiscleanup.core.commonassets.R
import com.crisiscleanup.core.commoncase.ui.CaseTableItem
import com.crisiscleanup.core.commoncase.ui.CasesAction
import com.crisiscleanup.core.commoncase.ui.CasesActionFlatButton
import com.crisiscleanup.core.commoncase.ui.FilterButtonBadge
import com.crisiscleanup.core.commoncase.ui.IncidentDropdownSelect
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupOutlinedButton
import com.crisiscleanup.core.designsystem.component.FormListSectionSeparator
import com.crisiscleanup.core.designsystem.component.HelpDialog
import com.crisiscleanup.core.designsystem.component.PhoneCallDialog
import com.crisiscleanup.core.designsystem.component.WorkTypeAction
import com.crisiscleanup.core.designsystem.component.WorkTypePrimaryAction
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.listItemVerticalPadding
import com.crisiscleanup.core.designsystem.theme.listRowItemStartPadding
import com.crisiscleanup.core.designsystem.theme.optionItemHeight
import com.crisiscleanup.core.model.data.TableWorksiteClaimAction
import com.crisiscleanup.core.model.data.TableWorksiteClaimStatus
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteSortBy
import com.crisiscleanup.feature.cases.CasesViewModel
import com.crisiscleanup.feature.cases.WorksiteDistance
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BoxScope.CasesTableView(
    viewModel: CasesViewModel = hiltViewModel(),
    isLoadingData: Boolean = false,
    isTableDataTransient: Boolean = false,
    @DrawableRes disasterResId: Int = R.drawable.ic_disaster_other,
    openIncidentSelect: () -> Unit = {},
    onCasesAction: (CasesAction) -> Unit = {},
    filtersCount: Int = 0,
    onTableItemSelect: (Worksite) -> Unit = {},
    onAssignCaseTeam: (Long) -> Unit = {},
    onSyncDataDelta: () -> Unit = {},
    onSyncDataFull: () -> Unit = {},
    hasIncidents: Boolean = false,
) {
    val countText by viewModel.casesCountTableText.collectAsStateWithLifecycle()

    val tableSort by viewModel.tableViewSort.collectAsStateWithLifecycle()
    val changeTableSort = remember(viewModel) {
        { sortBy: WorksiteSortBy -> viewModel.changeTableSort(sortBy) }
    }

    val selectedIncident by viewModel.selectedIncident.collectAsStateWithLifecycle()
    val isTurnOnRelease = selectedIncident.turnOnRelease

    val isEditable = !isTableDataTransient

    val onOpenFlags = remember(viewModel) {
        { worksite: Worksite -> viewModel.onOpenCaseFlags(worksite) }
    }

    var phoneNumberList by remember { mutableStateOf(emptyList<ParsedPhoneNumber>()) }
    val setPhoneNumberList = remember(viewModel) {
        { list: List<ParsedPhoneNumber> ->
            phoneNumberList = list
        }
    }

    val claimActionErrorMessage by viewModel.changeClaimActionErrorMessage.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Row(
            Modifier
                .listItemVerticalPadding()
                .listRowItemStartPadding()
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IncidentDropdownSelect(
                onOpenIncidents = openIncidentSelect,
                disasterIconResId = disasterResId,
                title = selectedIncident.shortName,
                contentDescription = selectedIncident.shortName,
                isLoading = isLoadingData,
                enabled = hasIncidents,
            )

            Spacer(Modifier.weight(1f))
            CasesActionFlatButton(
                CasesAction.Search,
                onCasesAction,
                isEditable,
            )
            FilterButtonBadge(filtersCount) {
                CasesActionFlatButton(
                    CasesAction.Filters,
                    onCasesAction,
                    isEditable,
                )
            }
        }

        Row(
            listItemModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = listItemSpacedByHalf,
        ) {
            AnimatedVisibility(
                visible = countText.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    countText,
                    Modifier.combinedClickable(
                        onClick = onSyncDataDelta,
                        onLongClick = onSyncDataFull,
                        enabled = !isLoadingData,
                    ),
                    style = LocalFontStyles.current.header4,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            TableViewSortSelect(
                tableSort,
                isEditable = !isLoadingData && isEditable,
                onChange = changeTableSort,
            )
        }

        val tableData by viewModel.tableData.collectAsStateWithLifecycle()
        val changingClaimIds by viewModel.worksitesChangingClaimAction.collectAsStateWithLifecycle()

        val tableSortMessage by viewModel.tableSortResultsMessage.collectAsStateWithLifecycle()
        if (tableSortMessage.isNotBlank()) {
            Text(
                tableSortMessage,
                listItemModifier,
                style = LocalFontStyles.current.header3,
            )
        }

        val onWorksiteClaimAction = remember(viewModel) {
            { worksite: Worksite, claimAction: TableWorksiteClaimAction ->
                viewModel.onWorksiteClaimAction(worksite, claimAction)
            }
        }

        val listState = rememberLazyListState()
        LaunchedEffect(tableSort) {
            if (viewModel.takeSortByChange()) {
                delay(150)
                listState.scrollToItem(0)
            }
        }
        LazyColumn(
            state = listState,
        ) {
            items(
                tableData,
                key = { it.worksite.id },
                contentType = { "table-item" },
            ) {
                val worksite = it.worksite
                val isChangingClaim = changingClaimIds.contains(worksite.id)
                TableViewItem(
                    it,
                    onViewCase = { onTableItemSelect(worksite) },
                    onOpenFlags = { onOpenFlags(worksite) },
                    onAssignTeam = { onAssignCaseTeam(worksite.id) },
                    isEditable = isEditable,
                    showPhoneNumbers = setPhoneNumberList,
                    isTurnOnRelease = isTurnOnRelease,
                    onWorksiteClaimAction = { claimAction: TableWorksiteClaimAction ->
                        onWorksiteClaimAction(worksite, claimAction)
                    },
                    isChangingClaim = isChangingClaim,
                )
                FormListSectionSeparator()
            }
        }
    }

    BusyIndicatorFloatingTopCenter(isTableDataTransient)

    val clearPhoneNumbers = remember(viewModel) { { setPhoneNumberList(emptyList()) } }
    PhoneCallDialog(
        phoneNumberList,
        clearPhoneNumbers,
    )

    var isClaimActionDialogVisible by remember(claimActionErrorMessage) { mutableStateOf(true) }
    if (claimActionErrorMessage.isNotBlank() && isClaimActionDialogVisible) {
        val dismissDialog =
            remember(claimActionErrorMessage) { { isClaimActionDialogVisible = false } }
        HelpDialog(
            title = LocalAppTranslator.current("info.error"),
            text = claimActionErrorMessage,
            onClose = dismissDialog,
        )
    }
}

private val sortByOptions = listOf(
    WorksiteSortBy.CaseNumber,
    WorksiteSortBy.Nearest,
    WorksiteSortBy.Name,
    WorksiteSortBy.City,
    WorksiteSortBy.CountyParish,
)

@Composable
private fun TableViewSortSelect(
    tableSort: WorksiteSortBy,
    isEditable: Boolean = false,
    onChange: (WorksiteSortBy) -> Unit = {},
) {
    val translator = LocalAppTranslator.current

    val sortText = translator(tableSort.translateKey)

    var showOptions by remember { mutableStateOf(false) }

    Box {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodySmall,
        ) {
            CrisisCleanupOutlinedButton(
                text = sortText,
                enabled = isEditable,
                onClick = { showOptions = true },
                fontWeight = FontWeight.W400,
            ) {
                Icon(
                    modifier = Modifier.offset(x = 16.dp),
                    imageVector = CrisisCleanupIcons.ArrowDropDown,
                    contentDescription = null,
                )
            }
        }

        val onSelect = { sortBy: WorksiteSortBy ->
            onChange(sortBy)
            showOptions = false
        }
        DropdownMenu(
            expanded = showOptions,
            onDismissRequest = { showOptions = false },
        ) {
            val selectedSort = if (tableSort == WorksiteSortBy.None) {
                WorksiteSortBy.CaseNumber
            } else {
                tableSort
            }
            for (option in sortByOptions) {
                key(option) {
                    DropdownMenuItem(
                        modifier = Modifier.optionItemHeight(),
                        text = {
                            val text = translator(option.translateKey)
                            Text(
                                text,
                                fontWeight = if (option == selectedSort) FontWeight.Bold else FontWeight.W400,
                            )
                        },
                        onClick = { onSelect(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TableViewItem(
    worksiteDistance: WorksiteDistance,
    onViewCase: () -> Unit = {},
    onOpenFlags: () -> Unit = {},
    onAssignTeam: (Long) -> Unit = {},
    isEditable: Boolean = false,
    showPhoneNumbers: (List<ParsedPhoneNumber>) -> Unit = {},
    isTurnOnRelease: Boolean = false,
    onWorksiteClaimAction: (TableWorksiteClaimAction) -> Unit = {},
    isChangingClaim: Boolean = false,
) {
    val t = LocalAppTranslator.current

    val worksite = worksiteDistance.worksite
    val distance = worksiteDistance.distanceMiles
    CaseTableItem(
        worksite,
        distance,
        // TODO Common dimensions
        Modifier.padding(16.dp),
        onViewCase = onViewCase,
        onOpenFlags = onOpenFlags,
        isEditable = isEditable,
        showPhoneNumbers = showPhoneNumbers,
        // TODO Profile recompose
        onAssignToTeam = { onAssignTeam(worksite.id) },
    ) {
        Spacer(modifier = Modifier.weight(1f))

        val isClaimActionEditable = isEditable && !isChangingClaim
        when (worksiteDistance.claimStatus) {
            TableWorksiteClaimStatus.HasUnclaimed -> {
                WorkTypePrimaryAction(
                    t("actions.claim"),
                    isClaimActionEditable,
                ) {
                    onWorksiteClaimAction(TableWorksiteClaimAction.Claim)
                }
            }

            TableWorksiteClaimStatus.ClaimedByMyOrg -> {
                WorkTypeAction(
                    t("actions.unclaim"),
                    isClaimActionEditable,
                ) {
                    onWorksiteClaimAction(TableWorksiteClaimAction.Unclaim)
                }
            }

            TableWorksiteClaimStatus.ClaimedByOthers -> {
                val isReleasable = isTurnOnRelease && worksite.isReleaseEligible
                val actionTextKey = if (isReleasable) {
                    "actions.release"
                } else {
                    "actions.request"
                }
                WorkTypeAction(t(actionTextKey), isClaimActionEditable) {
                    val action = if (isReleasable) {
                        TableWorksiteClaimAction.Release
                    } else {
                        TableWorksiteClaimAction.Request
                    }
                    onWorksiteClaimAction(action)
                }
            }

            TableWorksiteClaimStatus.Requested -> {
                Text(
                    t("caseView.requested"),
                    Modifier
                        .testTag("tableViewItemRequestedText")
                        .listItemVerticalPadding(),
                )
            }
        }
    }
}
