package com.crisiscleanup.feature.cases.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.designsystem.AppTranslator
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CollapsibleIcon
import com.crisiscleanup.core.designsystem.component.CrisisCleanupRadioButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox
import com.crisiscleanup.core.designsystem.component.HelpRow
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.component.WithHelpDialog
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listItemHeight
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.CasesFilterMaxDaysAgo
import com.crisiscleanup.core.model.data.CasesFilterMinDaysAgo
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.WorksiteFlagType
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.cases.CasesFilterViewModel
import com.crisiscleanup.feature.cases.CollapsibleFilterSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CasesFilterRoute(
    onBack: () -> Unit = {},
    viewModel: CasesFilterViewModel = hiltViewModel(),
) {
    val translator = viewModel.translator
    val appTranslator = remember(viewModel) {
        AppTranslator(translator = translator)
    }

    val updateFilters =
        remember(viewModel) { { filters: CasesFilter -> viewModel.changeFilters(filters) } }
    val workTypeStatuses by viewModel.workTypeStatuses.collectAsStateWithLifecycle()
    val workTypes by viewModel.workTypes.collectAsStateWithLifecycle(emptyList())

    val filters by viewModel.casesFilter.collectAsStateWithLifecycle()
    CompositionLocalProvider(
        LocalAppTranslator provides appTranslator,
    ) {
        Column(Modifier.fillMaxSize()) {
            TopAppBarBackAction(
                title = translator("worksiteFilters.filters"),
                onAction = onBack,
            )

            val sectionExpandState = remember { viewModel.sectionExpandState }
            val toggleCollapsible = { section: CollapsibleFilterSection ->
                sectionExpandState[section] = !sectionExpandState[section]!!
            }

            val closeKeyboard = rememberCloseKeyboard(viewModel)

            val updateWithinPrimary = remember(viewModel)
            {
                { b: Boolean -> updateFilters(filters.copy(isWithinPrimaryResponseArea = b)) }
            }
            val updateWithinSecondary = remember(viewModel)
            {
                { b: Boolean -> updateFilters(filters.copy(isWithinSecondaryResponseArea = b)) }
            }
            val updateAssignedToMyTeam = remember(viewModel)
            {
                { b: Boolean -> updateFilters(filters.copy(isAssignedToMyTeam = b)) }
            }
            val updateIsUnclaimed = remember(viewModel)
            {
                { b: Boolean -> updateFilters(filters.copy(isUnclaimed = b)) }
            }
            val updateIsClaimedByMyOrg = remember(viewModel)
            {
                { b: Boolean -> updateFilters(filters.copy(isClaimedByMyOrg = b)) }
            }
            val updateIsReportedByMyOrg = remember(viewModel)
            {
                { b: Boolean -> updateFilters(filters.copy(isReportedByMyOrg = b)) }
            }
            val updateIsStatusOpen = remember(viewModel)
            {
                { b: Boolean -> updateFilters(filters.copy(isStatusOpen = b)) }
            }
            val updateIsStatusClosed = remember(viewModel)
            {
                { b: Boolean -> updateFilters(filters.copy(isStatusClosed = b)) }
            }
            val updateWorkTypeStatus = remember(viewModel) {
                { status: WorkTypeStatus, b: Boolean ->
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
            }
            val updateMemberOfMyOrg = remember(viewModel)
            {
                { b: Boolean -> updateFilters(filters.copy(isMemberOfMyOrg = b)) }
            }
            val updateChildrenInHome = remember(viewModel)
            {
                { b: Boolean -> updateFilters(filters.copy(hasChildrenInHome = b)) }
            }
            val updateFirstResponder = remember(viewModel)
            {
                { b: Boolean -> updateFilters(filters.copy(isFirstResponder = b)) }
            }
            val updateOlderThan60 = remember(viewModel)
            {
                { b: Boolean -> updateFilters(filters.copy(isOlderThan60 = b)) }
            }
            val updateVeteran = remember(viewModel)
            {
                { b: Boolean -> updateFilters(filters.copy(isVeteran = b)) }
            }
            val updateFlags = remember(viewModel) {
                { flag: WorksiteFlagType, b: Boolean ->
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
            }
            val updateWorkTypes = remember(viewModel) {
                { workType: String, b: Boolean ->
                    val workTypes = filters.workTypes.toMutableSet()
                    if (b) {
                        workTypes.add(workType)
                    } else {
                        workTypes.remove(workType)
                    }
                    updateFilters(
                        filters.copy(workTypes = workTypes)
                    )
                }
            }
            val updateNoWorkType = remember(viewModel)
            {
                { b: Boolean -> updateFilters(filters.copy(isNoWorkType = b)) }
            }

            val isGeneralExpanded = sectionExpandState[CollapsibleFilterSection.General]!!
            LazyColumn(
                Modifier
                    .scrollFlingListener(closeKeyboard)
                    .weight(1f),
            ) {
                sviSlider(translator, filters, updateFilters)
                daysUpdatedSlider(translator, filters, updateFilters)
                distanceOptions(
                    translator,
                    filters,
                    updateFilters,
                    sectionExpandState[CollapsibleFilterSection.Distance]!!,
                    toggleCollapsible,
                    viewModel.distanceOptions,
                )
                generalOptions(
                    filters,
                    isGeneralExpanded,
                    toggleCollapsible,
                    updateWithinPrimary = updateWithinPrimary,
                    updateWithinSecondary = updateWithinSecondary,
                    updateTeamAssignment = updateAssignedToMyTeam,
                    updateIsUnclaimed = updateIsUnclaimed,
                    updateIsClaimedByMyOrg = updateIsClaimedByMyOrg,
                    updateIsReportedByMyOrg = updateIsReportedByMyOrg,
                    updateIsStatusOpen = updateIsStatusOpen,
                    workTypeStatuses = workTypeStatuses,
                    updateIsStatusClosed = updateIsStatusClosed,
                    updateWorkTypeStatus = updateWorkTypeStatus,
                )
                personalInfoOptions(
                    filters,
                    sectionExpandState[CollapsibleFilterSection.PersonalInfo]!!,
                    toggleCollapsible,
                    updateMemberOfMyOrg = updateMemberOfMyOrg,
                    updateChildrenInHome = updateChildrenInHome,
                    updateFirstResponder = updateFirstResponder,
                    updateOlderThan60 = updateOlderThan60,
                    updateVeteran = updateVeteran,
                )
                flagOptions(
                    filters,
                    sectionExpandState[CollapsibleFilterSection.Flags]!!,
                    toggleCollapsible,
                    viewModel.worksiteFlags,
                    updateFlags,
                )
                workOptions(
                    filters,
                    sectionExpandState[CollapsibleFilterSection.Work]!!,
                    toggleCollapsible,
                    workTypes,
                    updateWorkTypes,
                    updateNoWorkType,
                )
            }
        }
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
) {
    item {
        val translator = LocalAppTranslator.current.translator
        val label = translator(labelTranslateKey)
        Column(
            listItemModifier
                .then(modifier)
        ) {
            if (helpTranslateKey.isEmpty()) {
                Text(
                    label,
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
                Text(
                    minValueLabel,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    maxValueLabel,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun LazyListScope.sviSlider(
    translator: KeyResourceTranslator,
    filters: CasesFilter,
    onUpdateFilter: (CasesFilter) -> Unit = {}
) {
    rangeSliderItem(
        translator("svi.most_vulnerable"),
        translator("svi.everyone"),
        labelTranslateKey = "svi.vulnerability",
        value = filters.svi,
        onUpdate = { f: Float -> onUpdateFilter(filters.copy(svi = f)) },
        helpTranslateKey = "svi.svi_more_info_link",
        isHelpHtml = true,
    )
}

private fun LazyListScope.daysUpdatedSlider(
    translator: KeyResourceTranslator,
    filters: CasesFilter,
    onUpdateFilter: (CasesFilter) -> Unit = {}
) {
    rangeSliderItem(
        translator("worksiteFilters.days_ago").replace("{days}", CasesFilterMinDaysAgo.toString()),
        translator("worksiteFilters.days_ago").replace("{days}", CasesFilterMaxDaysAgo.toString()),
        // TODO Common dimensions
        modifier = Modifier.padding(top = 16.dp),
        labelTranslateKey = "worksiteFilters.updated",
        value = filters.daysAgoNormalized,
        onUpdate = { f: Float -> onUpdateFilter(filters.expandDaysAgo(f)) },
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
            style = MaterialTheme.typography.bodyLarge,
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

private fun LazyListScope.collapsibleSectionHeader(
    section: CollapsibleFilterSection,
    isSectionExpanded: Boolean = false,
    toggleSection: (CollapsibleFilterSection) -> Unit = {},
) {
    val translationKey = sectionTranslationKey[section] ?: ""
    item(
        key = "section-header-$section",
        contentType = "section-header",
    ) {
        FilterHeaderCollapsible(
            // TODO Common dimensions
            modifier = Modifier.padding(top = 16.dp),
            // TODO Map translation key
            sectionTitle = LocalAppTranslator.current.translator(translationKey),
            isCollapsed = !isSectionExpanded,
            toggleCollapse = { toggleSection(section) },
        )
    }
}

private fun LazyListScope.distanceOptions(
    translator: KeyResourceTranslator,
    filters: CasesFilter,
    onUpdateFilter: (CasesFilter) -> Unit = {},
    isSectionExpanded: Boolean = false,
    toggleSection: (CollapsibleFilterSection) -> Unit = {},
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
                    onSelect = { onUpdateFilter(filters.copy(distance = it.first)) }
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
            text = LocalAppTranslator.current.translator(translateKey),
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
            text = LocalAppTranslator.current.translator(textTranslateKey),
            checked = isChecked,
            onCheckChange = onCheckChange,
            onToggle = onToggle,
        )
    }
}

private fun LazyListScope.generalOptions(
    filters: CasesFilter,
    isSectionExpanded: Boolean = false,
    toggleSection: (CollapsibleFilterSection) -> Unit = {},
    updateWithinPrimary: (Boolean) -> Unit = {},
    updateWithinSecondary: (Boolean) -> Unit = {},
    updateTeamAssignment: (Boolean) -> Unit = {},
    updateIsUnclaimed: (Boolean) -> Unit = {},
    updateIsClaimedByMyOrg: (Boolean) -> Unit = {},
    updateIsReportedByMyOrg: (Boolean) -> Unit = {},
    updateIsStatusOpen: (Boolean) -> Unit = {},
    updateIsStatusClosed: (Boolean) -> Unit = {},
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

        checkboxItem(
            "worksiteFilters.open",
            filters.isStatusOpen,
            { b: Boolean -> updateIsStatusOpen(b) },
            { updateIsStatusOpen(!filters.isStatusOpen) },
        )

        checkboxItem(
            "worksiteFilters.closed",
            filters.isStatusClosed,
            { b: Boolean -> updateIsStatusClosed(b) },
            { updateIsStatusClosed(!filters.isStatusClosed) },
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
    toggleSection: (CollapsibleFilterSection) -> Unit = {},
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
    toggleSection: (CollapsibleFilterSection) -> Unit = {},
    flags: Collection<WorksiteFlagType> = emptyList(),
    updateWorksiteFlags: (WorksiteFlagType, Boolean) -> Unit = { _, _ -> },
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
                { b: Boolean -> updateWorksiteFlags(flag, b) },
                { updateWorksiteFlags(flag, !isChecked) },
            )
        }
    }
}

private fun LazyListScope.workOptions(
    filters: CasesFilter,
    isSectionExpanded: Boolean = false,
    toggleSection: (CollapsibleFilterSection) -> Unit = {},
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
