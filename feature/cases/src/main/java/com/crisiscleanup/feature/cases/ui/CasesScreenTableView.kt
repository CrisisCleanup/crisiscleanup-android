package com.crisiscleanup.feature.cases.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.common.ParsedPhoneNumber
import com.crisiscleanup.core.common.PhoneNumberUtil
import com.crisiscleanup.core.common.openDialer
import com.crisiscleanup.core.common.openMaps
import com.crisiscleanup.core.commonassets.R
import com.crisiscleanup.core.commoncase.model.addressQuery
import com.crisiscleanup.core.commoncase.ui.IncidentDropdownSelect
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupAlertDialog
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupOutlinedButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.FormListSectionSeparator
import com.crisiscleanup.core.designsystem.component.LinkifyPhoneText
import com.crisiscleanup.core.designsystem.component.WorkTypeAction
import com.crisiscleanup.core.designsystem.component.WorkTypePrimaryAction
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.attentionBackgroundColor
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.listItemVerticalPadding
import com.crisiscleanup.core.designsystem.theme.listRowItemStartPadding
import com.crisiscleanup.core.designsystem.theme.neutralIconColor
import com.crisiscleanup.core.designsystem.theme.optionItemHeight
import com.crisiscleanup.core.model.data.TableWorksiteClaimAction
import com.crisiscleanup.core.model.data.TableWorksiteClaimStatus
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteSortBy
import com.crisiscleanup.feature.cases.CasesViewModel
import com.crisiscleanup.feature.cases.WorksiteDistance
import kotlinx.coroutines.delay
import java.text.DecimalFormat

@Composable
internal fun BoxScope.CasesTableView(
    viewModel: CasesViewModel = hiltViewModel(),
    isIncidentLoading: Boolean = false,
    isTableDataTransient: Boolean = false,
    @DrawableRes disasterResId: Int = R.drawable.ic_disaster_other,
    openIncidentSelect: () -> Unit = {},
    onCasesAction: (CasesAction) -> Unit = {},
    filtersCount: Int = 0,
    casesCount: Int = 0,
    onTableItemSelect: (Worksite) -> Unit = {},
) {
    val translator = LocalAppTranslator.current

    val isTableBusy by viewModel.isTableBusy.collectAsStateWithLifecycle(false)

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

    var isWrongLocationDialogVisible by remember { mutableStateOf(false) }
    val showWrongLocationDialog = remember(viewModel) { { isWrongLocationDialogVisible = true } }
    val hideWrongLocationDialog = remember(viewModel) { { isWrongLocationDialogVisible = false } }

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
                isLoading = isIncidentLoading,
                enabled = isEditable,
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
            if (casesCount >= 0) {
                val caseCountText =
                    if (casesCount == 1) "$casesCount ${translator("casesVue.case")}"
                    else "$casesCount ${translator("casesVue.cases")}"
                Text(
                    caseCountText,
                    style = LocalFontStyles.current.header4,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            TableViewSortSelect(
                tableSort,
                isEditable = !(isIncidentLoading || isTableBusy || isTableDataTransient),
                onChange = changeTableSort
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
            delay(150)
            listState.scrollToItem(0)
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
                    isEditable = isEditable,
                    showPhoneNumbers = setPhoneNumberList,
                    showWrongLocationDialog = showWrongLocationDialog,
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

    BusyIndicatorFloatingTopCenter(isTableBusy || isTableDataTransient)

    PhoneNumbersDialog(
        parsedNumbers = phoneNumberList,
        setPhoneNumbers = setPhoneNumberList,
    )

    if (isWrongLocationDialogVisible) {
        CrisisCleanupAlertDialog(
            title = "~~Flagged",
            text = "~~This Case is marked as having a wrong location/address. It is advised to read all provided location and address information, notes, and/or make contact before traveling.",
            onDismissRequest = hideWrongLocationDialog,
            confirmButton = {
                CrisisCleanupTextButton(
                    text = LocalAppTranslator.current("actions.ok"),
                    onClick = hideWrongLocationDialog,
                )
            },
        )
    }

    var isClaimActionDialogVisible by remember(claimActionErrorMessage) { mutableStateOf(true) }
    if (claimActionErrorMessage.isNotBlank() && isClaimActionDialogVisible) {
        val dismissDialog =
            remember(claimActionErrorMessage) { { isClaimActionDialogVisible = false } }
        CrisisCleanupAlertDialog(
            title = "~~Error",
            text = claimActionErrorMessage,
            onDismissRequest = dismissDialog,
            confirmButton = {
                CrisisCleanupTextButton(
                    text = LocalAppTranslator.current("actions.ok"),
                    onClick = dismissDialog,
                )
            },
        )
    }
}

private val sortByOptions = listOf(
    WorksiteSortBy.Nearest,
    WorksiteSortBy.CaseNumber,
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
        // TODO: Dropdown where by distance asks for location permission
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodySmall
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
                    contentDescription = null
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
            val selectedSort = if (tableSort == WorksiteSortBy.None) WorksiteSortBy.CaseNumber
            else tableSort
            for (option in sortByOptions) {
                key(option) {
                    DropdownMenuItem(
                        modifier = Modifier.optionItemHeight(),
                        text = {
                            val text = translator(option.translateKey)
                            Text(
                                text,
                                fontWeight = if (option == selectedSort) FontWeight.Bold else FontWeight.W400
                            )
                        },
                        onClick = { onSelect(option) },
                    )
                }
            }
        }
    }
}

private val oneDecimalFormat = DecimalFormat("#.#")

@Composable
private fun TableViewItem(
    worksiteDistance: WorksiteDistance,
    onViewCase: () -> Unit = {},
    onOpenFlags: () -> Unit = {},
    isEditable: Boolean = false,
    showPhoneNumbers: (List<ParsedPhoneNumber>) -> Unit = {},
    showWrongLocationDialog: () -> Unit = {},
    isTurnOnRelease: Boolean = false,
    onWorksiteClaimAction: (TableWorksiteClaimAction) -> Unit = {},
    isChangingClaim: Boolean = false,
) {
    val translator = LocalAppTranslator.current

    val worksite = worksiteDistance.worksite
    val distance = worksiteDistance.distanceMiles
    val (fullAddress, geoQuery, locationQuery) = worksite.addressQuery

    Column(
        Modifier
            .clickable(
                onClick = onViewCase,
                enabled = isEditable,
            )
            // TODO Common dimensions
            .padding(16.dp),
        verticalArrangement = listItemSpacedBy,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = listItemSpacedBy,
        ) {
            Box(
                modifier = Modifier
                    .offset(x = (-8).dp)
                    // Similar to IconButton/IconButtonTokens.StateLayer*
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(
                        onClick = onOpenFlags,
                        enabled = isEditable,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val tint = LocalContentColor.current
                Icon(
                    painterResource(com.crisiscleanup.feature.cases.R.drawable.ic_flag_filled_small),
                    contentDescription = translator("nav.flag"),
                    tint = if (isEditable) tint else tint.disabledAlpha(),
                )
            }
            Text(
                worksite.caseNumber,
                modifier = Modifier.offset(x = (-14).dp),
                style = LocalFontStyles.current.header3,
            )

            Spacer(modifier = Modifier.weight(1f))

            if (distance >= 0) {
                val distanceText = oneDecimalFormat.format(distance)
                Row {
                    Text(
                        distanceText,
                        modifier = Modifier.padding(end = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        translator("~~mi"),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = listItemSpacedBy,
        ) {
            Icon(
                imageVector = CrisisCleanupIcons.Person,
                contentDescription = translator("phoneDashboard.name"),
                tint = neutralIconColor,
            )
            Text(worksite.name)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = listItemSpacedBy,
        ) {
            Icon(
                imageVector = CrisisCleanupIcons.Location,
                contentDescription = translator("profileOrg.address"),
                tint = neutralIconColor,
            )
            Text(
                fullAddress,
                Modifier.weight(1f),
            )
            if (worksite.hasWrongLocationFlag) {
                CrisisCleanupIconButton(
                    imageVector = CrisisCleanupIcons.Warning,
                    contentDescription = translator("flag.worksite_wrong_location_description"),
                    onClick = showWrongLocationDialog,
                    tint = attentionBackgroundColor,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = listItemSpacedBy,
        ) {
            val context = LocalContext.current
            CrisisCleanupOutlinedButton(
                onClick = {
                    val parsedNumbers =
                        PhoneNumberUtil.getPhoneNumbers(listOf(worksite.phone1, worksite.phone2))
                    if (parsedNumbers.size == 1 && parsedNumbers.first().parsedNumbers.size == 1) {
                        context.openDialer(parsedNumbers.first().parsedNumbers.first())
                    } else {
                        showPhoneNumbers(parsedNumbers)
                    }
                },
                enabled = isEditable && (worksite.phone1.isNotBlank() || worksite.phone2.isNotBlank()),
            ) {
                Icon(
                    imageVector = CrisisCleanupIcons.Phone,
                    contentDescription = translator("nav.phone"),
                )
            }

            CrisisCleanupOutlinedButton(
                onClick = {
                    val query = geoQuery.ifBlank { locationQuery }
                    context.openMaps(query)
                },
                enabled = isEditable && geoQuery.isNotBlank(),
            ) {
                Icon(
                    imageVector = CrisisCleanupIcons.Directions,
                    contentDescription = translator("~~Directions"),
                )
            }

            // TODO Implement add to team when team management is in play

            Spacer(modifier = Modifier.weight(1f))

            val isClaimActionEditable = isEditable && !isChangingClaim
            when (worksiteDistance.claimStatus) {
                TableWorksiteClaimStatus.HasUnclaimed -> {
                    WorkTypePrimaryAction(
                        translator("actions.claim"),
                        isClaimActionEditable,
                    ) {
                        onWorksiteClaimAction(TableWorksiteClaimAction.Claim)
                    }
                }

                TableWorksiteClaimStatus.ClaimedByMyOrg -> {
                    WorkTypeAction(
                        translator("actions.unclaim"),
                        isClaimActionEditable,
                    ) {
                        onWorksiteClaimAction(TableWorksiteClaimAction.Unclaim)
                    }
                }

                TableWorksiteClaimStatus.ClaimedByOthers -> {
                    val isReleasable = isTurnOnRelease && worksite.isReleaseEligible
                    val actionText = if (isReleasable) translator("actions.release")
                    else translator("actions.request")
                    WorkTypeAction(actionText, isClaimActionEditable) {
                        if (isReleasable) {
                            onWorksiteClaimAction(TableWorksiteClaimAction.Release)
                        } else {
                            onWorksiteClaimAction(TableWorksiteClaimAction.Request)
                        }
                    }
                }

                TableWorksiteClaimStatus.Requested -> {
                    Text(
                        translator("caseView.requested"),
                        Modifier.listItemVerticalPadding(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneNumbersDialog(
    parsedNumbers: List<ParsedPhoneNumber>,
    setPhoneNumbers: (List<ParsedPhoneNumber>) -> Unit,
) {
    if (parsedNumbers.flatMap(ParsedPhoneNumber::parsedNumbers).isNotEmpty()) {
        val dismissDialog = { setPhoneNumbers(emptyList()) }
        CrisisCleanupAlertDialog(
            title = "~~Contact Phone",
            onDismissRequest = dismissDialog,
            confirmButton = {
                CrisisCleanupTextButton(
                    text = LocalAppTranslator.current("actions.close"),
                    onClick = dismissDialog,
                )
            },
        ) {
            Column {
                for (parsedNumber in parsedNumbers) {
                    if (parsedNumber.parsedNumbers.isNotEmpty()) {
                        for (phoneNumber in parsedNumber.parsedNumbers) {
                            LinkifyPhoneText(
                                text = phoneNumber,
                                modifier = listItemModifier,
                            )
                        }
                    } else {
                        Text(
                            parsedNumber.source,
                            modifier = listItemModifier,
                        )
                    }
                }
            }
        }
    }
}