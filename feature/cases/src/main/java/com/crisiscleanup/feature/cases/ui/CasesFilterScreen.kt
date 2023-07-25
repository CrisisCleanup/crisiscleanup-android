package com.crisiscleanup.feature.cases.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.noonTime
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.CollapsibleIcon
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupRadioButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox
import com.crisiscleanup.core.designsystem.component.ExplainLocationPermissionDialog
import com.crisiscleanup.core.designsystem.component.FocusSectionSlider
import com.crisiscleanup.core.designsystem.component.FormListSectionSeparator
import com.crisiscleanup.core.designsystem.component.HelpRow
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.component.WithHelpDialog
import com.crisiscleanup.core.designsystem.component.actionHeight
import com.crisiscleanup.core.designsystem.component.cancelButtonColors
import com.crisiscleanup.core.designsystem.component.rememberFocusSectionSliderState
import com.crisiscleanup.core.designsystem.component.rememberSectionContentIndexLookup
import com.crisiscleanup.core.designsystem.component.roundedOutline
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listItemHeight
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.listItemVerticalPadding
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.CasesFilterMaxDaysAgo
import com.crisiscleanup.core.model.data.CasesFilterMinDaysAgo
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.WorksiteFlagType
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.cases.CasesFilterViewModel
import com.crisiscleanup.feature.cases.CollapsibleFilterSection
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd")
    .withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CasesFilterRoute(
    onBack: () -> Unit = {},
    viewModel: CasesFilterViewModel = hiltViewModel(),
) {
    val translator = viewModel.translator
    CompositionLocalProvider(
        LocalAppTranslator provides translator,
    ) {
        val filters by viewModel.casesFilters.collectAsStateWithLifecycle()
        val updateFilters =
            remember(viewModel) { { filters: CasesFilter -> viewModel.changeFilters(filters) } }

        Column(
            Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            TopAppBarBackAction(
                title = translator("worksiteFilters.filters"),
                onAction = onBack,
            )

            FilterControls(
                filters,
                updateFilters = updateFilters,
            )

            BottomActionBar(
                onBack = onBack,
                filters = filters,
            )
        }

        val closePermissionDialog =
            remember(viewModel) { { viewModel.showExplainPermissionLocation = false } }
        val explainPermission = viewModel.showExplainPermissionLocation
        ExplainLocationPermissionDialog(
            showDialog = explainPermission,
            closeDialog = closePermissionDialog,
            explanation = "~~Filtering by distance is not possible without GPS location access."
        )
    }
}

private val collapsibleFilterSections = listOf(
    CollapsibleFilterSection.Distance,
    CollapsibleFilterSection.General,
    CollapsibleFilterSection.PersonalInfo,
    CollapsibleFilterSection.Flags,
    CollapsibleFilterSection.Work,
    CollapsibleFilterSection.Dates,
)

@Composable
private fun ColumnScope.FilterControls(
    filters: CasesFilter,
    viewModel: CasesFilterViewModel = hiltViewModel(),
    updateFilters: (CasesFilter) -> Unit = {},
) {
    val translator = LocalAppTranslator.current

    val workTypeStatuses by viewModel.workTypeStatuses.collectAsStateWithLifecycle()
    val workTypes by viewModel.workTypes.collectAsStateWithLifecycle(emptyList())

    val filterSections = remember(viewModel) {
        collapsibleFilterSections.map {
            val translateKey = sectionTranslationKey[it]!!
            translator(translateKey)
        }
    }
    val sectionCollapseStates = remember(viewModel) {
        val collapseStates = SnapshotStateList<Boolean>()
        for (i in collapsibleFilterSections) {
            collapseStates.add(false)
        }
        collapseStates
    }

    val generalItemIndex = 7
    val personalInfoItemIndex = generalItemIndex + 13 + workTypeStatuses.size + 2
    val flagsItemIndex = personalInfoItemIndex + 9
    val workItemIndex = flagsItemIndex + viewModel.worksiteFlags.size + 2
    val datesItemIndex = workItemIndex + workTypes.size + 4
    val indexLookups by rememberSectionContentIndexLookup(
        mapOf(
            0 to 4,
            1 to generalItemIndex,
            2 to personalInfoItemIndex,
            3 to flagsItemIndex,
            4 to workItemIndex,
            5 to datesItemIndex,
        )
    )

    val sectionSliderState = rememberFocusSectionSliderState(
        viewModel,
        sectionCollapseStates,
        indexLookups,
    )

    FocusSectionSlider(
        filterSections,
        sectionSliderState,
        indexLookups,
        sectionCollapseStates,
    )

    val toggleSectionCollapse = remember(viewModel) {
        { sectionIndex: Int ->
            sectionCollapseStates[sectionIndex] = !sectionCollapseStates[sectionIndex]
        }
    }
    val toggleDistanceSection = remember(viewModel) { { toggleSectionCollapse(0) } }
    val toggleGeneralSection = remember(viewModel) { { toggleSectionCollapse(1) } }
    val togglePersonalInfoSection = remember(viewModel) { { toggleSectionCollapse(2) } }
    val toggleFlagsSection = remember(viewModel) { { toggleSectionCollapse(3) } }
    val toggleWorkSection = remember(viewModel) { { toggleSectionCollapse(4) } }
    val toggleDatesSection = remember(viewModel) { { toggleSectionCollapse(5) } }

    val closeKeyboard = rememberCloseKeyboard(viewModel)

    val updatedDaysDisplay = remember(viewModel) {
        { n: Float ->
            val daysAgo = CasesFilter.determineDaysAgo(n)
            "$daysAgo"
        }
    }

    val updateDistance = { distance: Float ->
        viewModel.tryChangeDistanceFilter(distance)
    }
    val updateWithinPrimary = { b: Boolean ->
        updateFilters(filters.copy(isWithinPrimaryResponseArea = b))
    }
    val updateWithinSecondary = { b: Boolean ->
        updateFilters(filters.copy(isWithinSecondaryResponseArea = b))
    }
    val updateAssignedToMyTeam = { b: Boolean ->
        updateFilters(filters.copy(isAssignedToMyTeam = b))
    }
    val updateIsUnclaimed = { b: Boolean -> updateFilters(filters.copy(isUnclaimed = b)) }
    val updateIsClaimedByMyOrg = { b: Boolean -> updateFilters(filters.copy(isClaimedByMyOrg = b)) }
    val updateIsReportedByMyOrg = { b: Boolean ->
        updateFilters(filters.copy(isReportedByMyOrg = b))
    }
    val updateOverallStatus = { isOpen: Boolean, isClosed: Boolean ->
        updateFilters(
            filters.copy(
                isStatusOpen = isOpen,
                isStatusClosed = isClosed,
            )
        )
    }
    val updateWorkTypeStatus = { status: WorkTypeStatus, b: Boolean ->
        val statuses = filters.workTypeStatuses.toMutableSet()
        if (b) {
            statuses.add(status)
        } else {
            statuses.remove(status)
        }
        updateFilters(
            filters.copy(workTypeStatuses = statuses)
        )
    }
    val updateMemberOfMyOrg = { b: Boolean -> updateFilters(filters.copy(isMemberOfMyOrg = b)) }
    val updateChildrenInHome = { b: Boolean -> updateFilters(filters.copy(hasChildrenInHome = b)) }
    val updateFirstResponder = { b: Boolean -> updateFilters(filters.copy(isFirstResponder = b)) }
    val updateOlderThan60 = { b: Boolean -> updateFilters(filters.copy(isOlderThan60 = b)) }
    val updateVeteran = { b: Boolean -> updateFilters(filters.copy(isVeteran = b)) }
    val updateFlags = { flag: WorksiteFlagType, b: Boolean ->
        val flags = filters.worksiteFlags.toMutableSet()
        if (b) {
            flags.add(flag)
        } else {
            flags.remove(flag)
        }
        updateFilters(
            filters.copy(worksiteFlags = flags)
        )
    }
    val updateWorkTypes = { workType: String, b: Boolean ->
        val modifiedWorkTypes = filters.workTypes.toMutableSet()
        if (b) {
            modifiedWorkTypes.add(workType)
        } else {
            modifiedWorkTypes.remove(workType)
        }
        updateFilters(
            filters.copy(workTypes = modifiedWorkTypes)
        )
    }
    val updateNoWorkType = { b: Boolean -> updateFilters(filters.copy(isNoWorkType = b)) }
    val updateCreatedAt = { dateRange: Pair<Instant, Instant>? ->
        updateFilters(filters.copy(createdAt = dateRange))
    }
    val updateUpdatedAt = { dateRange: Pair<Instant, Instant>? ->
        updateFilters(filters.copy(updatedAt = dateRange))
    }

    LazyColumn(
        Modifier
            .scrollFlingListener(closeKeyboard)
            .weight(1f)
            // TODO Common colors
            .background(Color.White),
        state = sectionSliderState.contentListState,
    ) {
        sviSlider(translator, filters, updateFilters)
        itemSectionSeparator()
        daysUpdatedSlider(translator, filters, updateFilters, updatedDaysDisplay)
        distanceOptions(
            filters,
            updateDistance,
            !sectionCollapseStates[0],
            toggleDistanceSection,
            viewModel.distanceOptions,
        )
        generalOptions(
            filters,
            !sectionCollapseStates[1],
            toggleGeneralSection,
            updateWithinPrimary = updateWithinPrimary,
            updateWithinSecondary = updateWithinSecondary,
            updateTeamAssignment = updateAssignedToMyTeam,
            updateIsUnclaimed = updateIsUnclaimed,
            updateIsClaimedByMyOrg = updateIsClaimedByMyOrg,
            updateIsReportedByMyOrg = updateIsReportedByMyOrg,
            updateOverallStatus = updateOverallStatus,
            workTypeStatuses = workTypeStatuses,
            updateWorkTypeStatus = updateWorkTypeStatus,
        )
        personalInfoOptions(
            filters,
            !sectionCollapseStates[2],
            togglePersonalInfoSection,
            updateMemberOfMyOrg = updateMemberOfMyOrg,
            updateChildrenInHome = updateChildrenInHome,
            updateFirstResponder = updateFirstResponder,
            updateOlderThan60 = updateOlderThan60,
            updateVeteran = updateVeteran,
        )
        flagOptions(
            filters,
            !sectionCollapseStates[3],
            toggleFlagsSection,
            viewModel.worksiteFlags,
            updateFlags = updateFlags,
        )
        workOptions(
            filters,
            !sectionCollapseStates[4],
            toggleWorkSection,
            workTypes,
            updateWorkTypes = updateWorkTypes,
            updateNoWorkType = updateNoWorkType,
        )
        dateOptions(
            filters,
            !sectionCollapseStates[5],
            toggleDatesSection,
            updateCreatedAt = updateCreatedAt,
            updateUpdatedAt = updateUpdatedAt,
        )
    }
}

private fun LazyListScope.rangeSliderItem(
    minValueLabel: String,
    maxValueLabel: String,
    modifier: Modifier = Modifier,
    labelTranslateKey: String = "",
    value: Float = 1f,
    onUpdate: (Float) -> Unit = {},
    helpTranslateKey: String = "",
    isHelpHtml: Boolean = false,
    currentValueDisplay: (Float) -> String = { "" },
) {
    item {
        val translator = LocalAppTranslator.current
        val label = translator(labelTranslateKey)
        Column(
            listItemModifier
                .then(modifier)
        ) {
            if (helpTranslateKey.isEmpty()) {
                val currentValue = currentValueDisplay(value)
                val text = if (currentValue.isBlank()) label else "$label ($currentValue)"
                Text(
                    text,
                    modifier = Modifier.listItemVerticalPadding(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                WithHelpDialog(
                    translator,
                    helpTitle = label,
                    helpText = translator(helpTranslateKey),
                    hasHtml = isHelpHtml,
                ) { showHelp ->
                    HelpRow(
                        text = label,
                        iconContentDescription = label,
                        showHelp = showHelp,
                        isBold = true,
                    )
                }
            }

            Slider(
                value = value,
                onValueChange = onUpdate,
            )

            Row(Modifier.fillMaxWidth()) {
                val textStyle = MaterialTheme.typography.bodyMedium
                Text(
                    minValueLabel,
                    style = textStyle,
                )
                Spacer(Modifier.weight(1f))

                Text(
                    maxValueLabel,
                    style = textStyle,
                )
            }
        }
    }
}

private fun LazyListScope.sviSlider(
    translator: KeyResourceTranslator,
    filters: CasesFilter,
    onUpdateFilter: (CasesFilter) -> Unit = {},
    sviValueDisplay: (Float) -> String = { "" },
) {
    rangeSliderItem(
        translator("svi.most_vulnerable"),
        translator("svi.everyone"),
        labelTranslateKey = "svi.vulnerability",
        value = filters.svi,
        onUpdate = { f: Float -> onUpdateFilter(filters.copy(svi = f)) },
        helpTranslateKey = "svi.svi_more_info_link",
        isHelpHtml = true,
        currentValueDisplay = sviValueDisplay,
    )
}

private fun LazyListScope.daysUpdatedSlider(
    translator: KeyResourceTranslator,
    filters: CasesFilter,
    onUpdateFilter: (CasesFilter) -> Unit = {},
    daysUpdatedDisplay: (Float) -> String = { "" },
) {
    rangeSliderItem(
        translator("worksiteFilters.days_ago").replace("{days}", CasesFilterMinDaysAgo.toString()),
        translator("worksiteFilters.days_ago").replace("{days}", CasesFilterMaxDaysAgo.toString()),
        labelTranslateKey = "worksiteFilters.updated",
        value = filters.daysAgoNormalized,
        onUpdate = { f: Float -> onUpdateFilter(filters.expandDaysAgo(f)) },
        currentValueDisplay = daysUpdatedDisplay,
    )
}

@Composable
private fun FilterHeaderCollapsible(
    modifier: Modifier = Modifier,
    sectionTitle: String,
    isCollapsed: Boolean = false,
    toggleCollapse: () -> Unit = {},
) {
    Row(
        modifier
            .clickable(onClick = toggleCollapse)
            .listItemHeight()
            .listItemPadding(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            sectionTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        val iconVector =
            if (isCollapsed) CrisisCleanupIcons.ExpandLess
            else CrisisCleanupIcons.ExpandMore
        Spacer(Modifier.weight(1f))

        CollapsibleIcon(isCollapsed, sectionTitle, iconVector)
    }
}

private val sectionTranslationKey = mapOf(
    CollapsibleFilterSection.Distance to "worksiteFilters.distance",
    CollapsibleFilterSection.General to "worksiteFilters.general",
    CollapsibleFilterSection.PersonalInfo to "worksiteFilters.personal_info",
    CollapsibleFilterSection.Flags to "worksiteFilters.flags",
    CollapsibleFilterSection.Work to "worksiteFilters.work",
    CollapsibleFilterSection.Dates to "worksiteFilters.dates",
)

private fun LazyListScope.itemSectionSeparator() {
    item(contentType = "section-separator") {
        FormListSectionSeparator()
    }
}

private fun LazyListScope.collapsibleSectionHeader(
    section: CollapsibleFilterSection,
    isSectionExpanded: Boolean = false,
    toggleSection: () -> Unit = {},
    isFirstSection: Boolean = false,
) {
    if (!isFirstSection) {
        itemSectionSeparator()
    }

    val translationKey = sectionTranslationKey[section] ?: ""
    item(
        key = "section-header-$section",
        contentType = "section-header",
    ) {
        FilterHeaderCollapsible(
            sectionTitle = LocalAppTranslator.current(translationKey),
            isCollapsed = !isSectionExpanded,
            toggleCollapse = toggleSection,
        )
    }
}

private fun LazyListScope.distanceOptions(
    filters: CasesFilter,
    updateDistance: (Float) -> Unit = {},
    isSectionExpanded: Boolean = false,
    toggleSection: () -> Unit = {},
    options: List<Pair<Float, String>> = emptyList(),
) {
    collapsibleSectionHeader(
        CollapsibleFilterSection.Distance,
        isSectionExpanded,
        toggleSection,
    )

    if (isSectionExpanded) {
        item {
            options.forEach {
                CrisisCleanupRadioButton(
                    listItemModifier,
                    selected = filters.distance == it.first,
                    text = it.second,
                    onSelect = { updateDistance(it.first) }
                )
            }
        }
    }
}

private fun LazyListScope.subsectionHeader(
    translateKey: String,
) {
    item(contentType = "subsection-header") {
        Text(
            text = LocalAppTranslator.current(translateKey),
            modifier = listItemModifier,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun LazyListScope.checkboxItem(
    textTranslateKey: String,
    isChecked: Boolean,
    onCheckChange: (Boolean) -> Unit,
    onToggle: () -> Unit,
) {
    item(contentType = "filter-checkbox") {
        CrisisCleanupTextCheckbox(
            listItemModifier,
            text = LocalAppTranslator.current(textTranslateKey),
            checked = isChecked,
            onCheckChange = onCheckChange,
            onToggle = onToggle,
        )
    }
}

private fun LazyListScope.generalOptions(
    filters: CasesFilter,
    isSectionExpanded: Boolean = false,
    toggleSection: () -> Unit = {},
    updateWithinPrimary: (Boolean) -> Unit = {},
    updateWithinSecondary: (Boolean) -> Unit = {},
    updateTeamAssignment: (Boolean) -> Unit = {},
    updateIsUnclaimed: (Boolean) -> Unit = {},
    updateIsClaimedByMyOrg: (Boolean) -> Unit = {},
    updateIsReportedByMyOrg: (Boolean) -> Unit = {},
    updateOverallStatus: (Boolean, Boolean) -> Unit = { _, _ -> },
    workTypeStatuses: Collection<WorkTypeStatus> = emptyList(),
    updateWorkTypeStatus: (WorkTypeStatus, Boolean) -> Unit = { _, _ -> },
) {
    collapsibleSectionHeader(
        CollapsibleFilterSection.General,
        isSectionExpanded,
        toggleSection,
    )

    if (isSectionExpanded) {
        subsectionHeader("worksiteFilters.location")

        checkboxItem(
            "worksiteFilters.in_primary_response_area",
            filters.isWithinPrimaryResponseArea,
            { b: Boolean -> updateWithinPrimary(b) },
            { updateWithinPrimary(!filters.isWithinPrimaryResponseArea) },
        )

        checkboxItem(
            "worksiteFilters.in_secondary_response_area",
            filters.isWithinSecondaryResponseArea,
            { b: Boolean -> updateWithinSecondary(b) },
            { updateWithinSecondary(!filters.isWithinSecondaryResponseArea) },
        )

        subsectionHeader("worksiteFilters.team")

        checkboxItem(
            "worksiteFilters.assigned_to_my_team",
            filters.isAssignedToMyTeam,
            { b: Boolean -> updateTeamAssignment(b) },
            { updateTeamAssignment(!filters.isAssignedToMyTeam) },
        )

        subsectionHeader("worksiteFilters.claim_reported_by")

        checkboxItem(
            "worksiteFilters.unclaimed",
            filters.isUnclaimed,
            { b: Boolean -> updateIsUnclaimed(b) },
            { updateIsUnclaimed(!filters.isUnclaimed) },
        )

        checkboxItem(
            "worksiteFilters.claimed_by_my_org",
            filters.isClaimedByMyOrg,
            { b: Boolean -> updateIsClaimedByMyOrg(b) },
            { updateIsClaimedByMyOrg(!filters.isClaimedByMyOrg) },
        )

        checkboxItem(
            "worksiteFilters.reported_by_my_org",
            filters.isReportedByMyOrg,
            { b: Boolean -> updateIsReportedByMyOrg(b) },
            { updateIsReportedByMyOrg(!filters.isReportedByMyOrg) },
        )

        subsectionHeader("worksiteFilters.over_all_status")

        val isStatusOpen = filters.isStatusOpen
        checkboxItem(
            "worksiteFilters.open",
            isStatusOpen,
            { b: Boolean -> updateOverallStatus(b, false) },
            { updateOverallStatus(!isStatusOpen, false) },
        )

        val isStatusClosed = !isStatusOpen && filters.isStatusClosed
        checkboxItem(
            "worksiteFilters.closed",
            isStatusClosed,
            { b: Boolean -> updateOverallStatus(false, b) },
            { updateOverallStatus(false, !isStatusClosed) },
        )

        subsectionHeader("worksiteFilters.detailed_status")

        for (status in workTypeStatuses) {
            val isChecked = filters.workTypeStatuses.contains(status)
            checkboxItem(
                status.literal,
                isChecked,
                { b: Boolean -> updateWorkTypeStatus(status, b) },
                { updateWorkTypeStatus(status, !isChecked) },
            )
        }
    }
}

private fun LazyListScope.personalInfoOptions(
    filters: CasesFilter,
    isSectionExpanded: Boolean = false,
    toggleSection: () -> Unit = {},
    updateMemberOfMyOrg: (Boolean) -> Unit = {},
    updateOlderThan60: (Boolean) -> Unit = {},
    updateChildrenInHome: (Boolean) -> Unit = {},
    updateFirstResponder: (Boolean) -> Unit = {},
    updateVeteran: (Boolean) -> Unit = {},
) {
    collapsibleSectionHeader(
        CollapsibleFilterSection.PersonalInfo,
        isSectionExpanded,
        toggleSection,
    )

    if (isSectionExpanded) {
        subsectionHeader("worksiteFilters.my_organization")

        checkboxItem(
            "worksiteFilters.member_of_my_org",
            filters.isMemberOfMyOrg,
            { b: Boolean -> updateMemberOfMyOrg(b) },
            { updateMemberOfMyOrg(!filters.isMemberOfMyOrg) },
        )

        subsectionHeader("worksiteFilters.personal_info")

        checkboxItem(
            "formLabels.older_than_60",
            filters.isOlderThan60,
            { b: Boolean -> updateOlderThan60(b) },
            { updateOlderThan60(!filters.isOlderThan60) },
        )

        checkboxItem(
            "formLabels.children_in_home",
            filters.hasChildrenInHome,
            { b: Boolean -> updateChildrenInHome(b) },
            { updateChildrenInHome(!filters.hasChildrenInHome) },
        )

        checkboxItem(
            "formLabels.first_responder",
            filters.isFirstResponder,
            { b: Boolean -> updateFirstResponder(b) },
            { updateFirstResponder(!filters.isFirstResponder) },
        )

        checkboxItem(
            "formLabels.veteran",
            filters.isVeteran,
            { b: Boolean -> updateVeteran(b) },
            { updateVeteran(!filters.isVeteran) },
        )
    }
}

private fun LazyListScope.flagOptions(
    filters: CasesFilter,
    isSectionExpanded: Boolean = false,
    toggleSection: () -> Unit = {},
    flags: Collection<WorksiteFlagType> = emptyList(),
    updateFlags: (WorksiteFlagType, Boolean) -> Unit = { _, _ -> },
) {
    collapsibleSectionHeader(
        CollapsibleFilterSection.Flags,
        isSectionExpanded,
        toggleSection,
    )

    if (isSectionExpanded) {
        for (flag in flags) {
            val isChecked = filters.worksiteFlags.contains(flag)
            checkboxItem(
                flag.literal,
                isChecked,
                { b: Boolean -> updateFlags(flag, b) },
                { updateFlags(flag, !isChecked) },
            )
        }
    }
}

private fun LazyListScope.workOptions(
    filters: CasesFilter,
    isSectionExpanded: Boolean = false,
    toggleSection: () -> Unit = {},
    workTypes: Collection<String> = emptyList(),
    updateWorkTypes: (String, Boolean) -> Unit = { _, _ -> },
    updateNoWorkType: (Boolean) -> Unit = {},
) {
    collapsibleSectionHeader(
        CollapsibleFilterSection.Work,
        isSectionExpanded,
        toggleSection,
    )

    if (isSectionExpanded) {
        for (workType in workTypes) {
            val isChecked = filters.workTypes.contains(workType)
            checkboxItem(
                "workType.${workType}",
                isChecked,
                { b: Boolean -> updateWorkTypes(workType, b) },
                { updateWorkTypes(workType, !isChecked) },
            )
        }

        subsectionHeader("worksiteFilters.missing_information")

        checkboxItem(
            "worksiteFilters.no_work_type",
            filters.isNoWorkType,
            { b: Boolean -> updateNoWorkType(b) },
            { updateNoWorkType(!filters.isNoWorkType) },
        )
    }
}

private fun LazyListScope.dateItem(
    textTranslateKey: String,
    dateRange: Pair<Instant, Instant>?,
    onDateChange: (Pair<Instant, Instant>?) -> Unit,
) {
    item(contentType = "filter-date") {
        val translator = LocalAppTranslator.current
        val label = translator(textTranslateKey)
        Text(
            label,
            Modifier.listItemHorizontalPadding(),
            style = MaterialTheme.typography.bodyLarge,
        )
        FilterDatePicker(
            label,
            listItemModifier,
            dateRange = dateRange,
            onDateChange = onDateChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDatePicker(
    label: String,
    modifier: Modifier = Modifier,
    dateRange: Pair<Instant, Instant>? = null,
    onDateChange: (Pair<Instant, Instant>?) -> Unit = {},
) {
    var showDatePicker by remember { mutableStateOf(false) }
    Row(
        Modifier
            .clickable(onClick = { showDatePicker = true })
            .then(modifier)
            .roundedOutline()
            .listItemHorizontalPadding(),
        horizontalArrangement = listItemSpacedByHalf,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dateText = dateRange?.let {
            val startDate = dateFormatter.format(it.first.toJavaInstant())
            val endDate = dateFormatter.format(it.second.toJavaInstant())
            "$startDate - $endDate"
        } ?: ""
        Text(dateText)
        Spacer(
            Modifier
                .weight(1f)
                .actionHeight()
        )
        if (dateRange != null) {
            CrisisCleanupIconButton(
                imageVector = CrisisCleanupIcons.Clear,
                onClick = { onDateChange(null) }
            )
        }
        Icon(
            imageVector = CrisisCleanupIcons.Calendar,
            contentDescription = label,
        )
    }
    if (showDatePicker) {
        com.crisiscleanup.core.designsystem.component.DateRangePickerDialog(
            selectedMillis = dateRange?.let {
                Pair(
                    it.first.toEpochMilliseconds(),
                    it.second.toEpochMilliseconds()
                )
            },
            onCloseDialog = { selectedMillis ->
                val selectedDateRange = selectedMillis?.let {
                    val startDate = Instant.fromEpochMilliseconds(it.first).noonTime
                    val endDate = Instant.fromEpochMilliseconds(it.second).noonTime
                    Pair(startDate, endDate)
                }
                onDateChange(selectedDateRange)
                showDatePicker = false
            }
        )
    }
}

private fun LazyListScope.dateOptions(
    filters: CasesFilter,
    isSectionExpanded: Boolean = false,
    toggleSection: () -> Unit = {},
    updateCreatedAt: (Pair<Instant, Instant>?) -> Unit = {},
    updateUpdatedAt: (Pair<Instant, Instant>?) -> Unit = {},
) {
    collapsibleSectionHeader(
        CollapsibleFilterSection.Dates,
        isSectionExpanded,
        toggleSection,
    )

    if (isSectionExpanded) {
        dateItem(
            "worksiteFilters.created",
            filters.createdAt,
        ) { dateRange: Pair<Instant, Instant>? ->
            updateCreatedAt(dateRange)
        }

        dateItem(
            "worksiteFilters.updated",
            filters.updatedAt,
        ) { dateRange: Pair<Instant, Instant>? ->
            updateUpdatedAt(dateRange)
        }
    }
}

@Composable
fun BottomActionBar(
    onBack: () -> Unit,
    filters: CasesFilter,
    viewModel: CasesFilterViewModel = hiltViewModel(),
) {
    val translator = LocalAppTranslator.current
    Row(
        modifier = listItemModifier,
        horizontalArrangement = listItemSpacedBy,
    ) {
        val filterCount = filters.changeCount
        val hasFilters = filterCount > 0
        BusyButton(
            Modifier.weight(1f),
            text = translator("actions.clear_filters"),
            enabled = hasFilters,
            onClick = viewModel::clearFilters,
            colors = cancelButtonColors(),
        )
        val applyFilters = translator("actions.apply_filters")
        val applyText = if (hasFilters) "$applyFilters ($filterCount)" else applyFilters
        BusyButton(
            Modifier.weight(1f),
            text = applyText,
            enabled = hasFilters,
            onClick = {
                viewModel.applyFilters(filters)
                onBack()
            },
        )
    }
}

