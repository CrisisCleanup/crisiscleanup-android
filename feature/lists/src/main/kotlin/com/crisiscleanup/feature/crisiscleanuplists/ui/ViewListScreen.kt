package com.crisiscleanup.feature.crisiscleanuplists.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.common.ParsedPhoneNumber
import com.crisiscleanup.core.common.relativeTime
import com.crisiscleanup.core.commonassets.getDisasterIcon
import com.crisiscleanup.core.commoncase.model.addressQuery
import com.crisiscleanup.core.commoncase.ui.ExplainWrongLocationDialog
import com.crisiscleanup.core.commoncase.ui.IncidentHeaderView
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifierNone
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupAlertDialog
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.LinkifyEmailText
import com.crisiscleanup.core.designsystem.component.LinkifyPhoneText
import com.crisiscleanup.core.designsystem.component.PhoneCallDialog
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.component.WorksiteAddressButton
import com.crisiscleanup.core.designsystem.component.WorksiteAddressView
import com.crisiscleanup.core.designsystem.component.WorksiteCallButton
import com.crisiscleanup.core.designsystem.component.WorksiteNameView
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemCenterSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.listItemHeight
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.model.data.CrisisCleanupList
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.IncidentOrganization
import com.crisiscleanup.core.model.data.ListModel
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.crisiscleanuplists.ViewListViewModel
import com.crisiscleanup.feature.crisiscleanuplists.ViewListViewState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ViewListRoute(
    onBack: () -> Unit = {},
    onOpenList: (Long) -> Unit = {},
    onOpenWorksite: (ExistingWorksiteIdentifier) -> Unit,
    viewModel: ViewListViewModel = hiltViewModel(),
) {
    val t = LocalAppTranslator.current

    val screenTitle by viewModel.screenTitle.collectAsStateWithLifecycle()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    val isConfirmingOpenWorksite = viewModel.isConfirmingOpenWorksite
    val openWorksiteId = viewModel.openWorksiteId
    if (openWorksiteId != ExistingWorksiteIdentifierNone) {
        onOpenWorksite(openWorksiteId)
        viewModel.openWorksiteId = ExistingWorksiteIdentifierNone
    }

    val isChangingIncident = viewModel.isChangingIncident

    val indicateLoading = viewState is ViewListViewState.Loading ||
            isConfirmingOpenWorksite ||
            isChangingIncident

    Box(Modifier.fillMaxSize()) {
        Column {
            TopAppBarBackAction(
                title = screenTitle,
                onAction = onBack,
            )

            when (viewState) {
                is ViewListViewState.Success -> {
                    val successState = (viewState as ViewListViewState.Success)
                    val list = successState.list
                    val objectData = successState.objectData
                    ListDetailsView(
                        list,
                        objectData,
                        onOpenList,
                        viewModel::onOpenWorksite,
                        rememberKey = viewModel,
                    )
                }

                is ViewListViewState.Error -> {
                    Text((viewState as ViewListViewState.Error).message)
                }

                else -> {}
            }
        }

        BusyIndicatorFloatingTopCenter(indicateLoading)

        val openWorksiteError = viewModel.openWorksiteError
        if (openWorksiteError.isNotBlank()) {
            val closeDialog = remember(viewModel) { { viewModel.openWorksiteError = "" } }
            CrisisCleanupAlertDialog(
                title = t("info.error"),
                text = openWorksiteError,
                onDismissRequest = closeDialog,
                confirmButton = {
                    CrisisCleanupTextButton(
                        text = t("actions.close"),
                        onClick = closeDialog,
                    )
                },
            )
        }

        val changeIncidentConfirmMessage = viewModel.changeIncidentConfirmMessage
        if (changeIncidentConfirmMessage.isNotBlank()) {
            val closeDialog = viewModel::clearChangeIncident
            CrisisCleanupAlertDialog(
                title = t("~~Confirm change Incident"),
                text = changeIncidentConfirmMessage,
                onDismissRequest = closeDialog,
                confirmButton = {
                    CrisisCleanupTextButton(
                        text = t("actions.continue"),
                        onClick = viewModel::onConfirmChangeIncident,
                    )
                },
                dismissButton = {
                    CrisisCleanupTextButton(
                        text = t("actions.cancel"),
                        onClick = closeDialog,
                    )
                },
            )
        }
    }
}

@Composable
private fun ListDetailsView(
    list: CrisisCleanupList,
    objectData: List<Any?>,
    onOpenList: (Long) -> Unit,
    onOpenWorksite: (Worksite) -> Unit,
    rememberKey: Any,
) {
    val t = LocalAppTranslator.current

    if (objectData.isEmpty()) {
        Text(
            t("~~This list is not supported by the app or has no items."),
            listItemModifier,
        )
    } else {
        var phoneNumberList by remember { mutableStateOf(emptyList<ParsedPhoneNumber>()) }
        val setPhoneNumberList = remember(rememberKey) {
            { list: List<ParsedPhoneNumber> ->
                phoneNumberList = list
            }
        }
        val clearPhoneNumbers = remember(rememberKey) { { setPhoneNumberList(emptyList()) } }
        PhoneCallDialog(
            parsedNumbers = phoneNumberList,
            onCloseDialog = clearPhoneNumbers,
        )

        val openList = remember(rememberKey) {
            { list: CrisisCleanupList ->
                onOpenList(list.id)
            }
        }

        LazyColumn(verticalArrangement = listItemCenterSpacedByHalf) {
            item {
                list.incident?.let { incident ->
                    IncidentHeaderView(
                        Modifier,
                        incident.shortName,
                        getDisasterIcon(incident.disaster),
                        isSyncing = false,
                    )
                }

                Row(
                    listItemModifier,
                    horizontalArrangement = listItemSpacedByHalf,
                ) {
                    ListIcon(list)
                    Text(list.updatedAt.relativeTime)
                }

                val description = list.description.trim()
                if (description.isNotBlank()) {
                    Text(
                        description,
                        listItemModifier,
                    )
                }
            }

            when (list.model) {
                ListModel.Incident -> {
                    incidentItems(objectData)
                }

                ListModel.List -> {
                    listItems(
                        objectData,
                        openList,
                    )
                }

                ListModel.Organization -> {
                    organizationItems(objectData)
                }

                ListModel.User -> {
                    userItems(objectData)
                }

                ListModel.Worksite -> {
                    worksiteItems(
                        list.incident?.id ?: EmptyIncident.id,
                        objectData,
                        setPhoneNumberList,
                        onOpenWorksite,
                    )
                }

                else -> {
                    item {
                        Text(t("~~This list is not supported by the app."))
                    }
                }
            }
        }
    }
}

@Composable
private fun MissingItem() {
    Box(
        listItemModifier.listItemHeight(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            LocalAppTranslator.current("~~Missing list data."),
        )
    }
}

private fun LazyListScope.incidentItems(
    listData: List<Any?>,
) {
    // TODO Test when issues are fixed
    val incidents = listData.map { it as? Incident }
    items(
        incidents.size,
        key = { incidents[it]?.id ?: -it },
        contentType = { incidents[it]?.id ?: "missing-item" },
    ) {
        val incident = incidents[it]
        if (incident == null) {
            MissingItem()
        } else {
            IncidentHeaderView(
                Modifier,
                incident.shortName,
                getDisasterIcon(incident.disaster),
                isSyncing = false,
            )
        }
    }
}

private fun LazyListScope.listItems(
    listData: List<Any?>,
    onOpenList: (CrisisCleanupList) -> Unit = {},
) {
    val lists = listData.map { it as? CrisisCleanupList }
    items(
        lists.size,
        key = { lists[it]?.id ?: -it },
        contentType = { lists[it]?.id ?: "missing-item" },
    ) {
        val list = lists[it]
        if (list == null) {
            MissingItem()
        } else {
            ListItemSummaryView(
                list,
                Modifier
                    .clickable {
                        onOpenList(list)
                    }
                    .then(listItemModifier),
                true,
            )
        }
    }
}

private fun LazyListScope.organizationItems(
    listData: List<Any?>,
) {
    val organizations = listData.map { it as? IncidentOrganization }
    items(
        organizations.size,
        key = { organizations[it]?.id ?: -it },
        contentType = { organizations[it]?.id ?: "missing-item" },
    ) {
        val organization = organizations[it]
        if (organization == null) {
            MissingItem()
        } else {
            Text(
                organization.name,
                listItemModifier
                    .listItemHeight()
                    .wrapContentHeight(align = Alignment.CenterVertically),
            )
        }
    }
}

private fun LazyListScope.userItems(
    listData: List<Any?>,
) {
    // TODO Test when issues are fixed
    val users = listData.map { it as? PersonContact }
    items(
        users.size,
        key = { users[it]?.id ?: -it },
        contentType = { users[it]?.id ?: "missing-item" },
    ) {
        val contact = users[it]
        if (contact == null) {
            MissingItem()
        } else {
            Column(
                listItemModifier.listItemHeight(),
                verticalArrangement = listItemCenterSpacedByHalf,
            ) {
                Text(contact.fullName)
                if (contact.mobile.isNotBlank()) {
                    LinkifyPhoneText(contact.mobile)
                }
                if (contact.email.isNotBlank()) {
                    LinkifyEmailText(contact.email)
                }
            }
        }
    }
}

private fun LazyListScope.worksiteItems(
    incidentId: Long,
    listData: List<Any?>,
    showPhoneNumbers: (List<ParsedPhoneNumber>) -> Unit,
    onOpenWorksite: (Worksite) -> Unit = {},
) {
    val worksites = listData.map { it as? Worksite }
    items(
        worksites.size,
        key = { worksites[it]?.id ?: -it },
        contentType = { worksites[it]?.id ?: "missing-item" },
    ) {
        val worksite = worksites[it]
        if (worksite == null || worksite == EmptyWorksite) {
            MissingItem()
        } else if (worksite.incidentId != incidentId) {
            Box(
                listItemModifier.listItemHeight(),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    LocalAppTranslator.current("~~Case {case_number} is not under this Incident.")
                        .replace("{case_number}", worksite.caseNumber),
                )
            }
        } else {
            val (fullAddress, geoQuery, locationQuery) = worksite.addressQuery

            Column(
                Modifier
                    .clickable(onClick = { onOpenWorksite(worksite) })
                    .then(listItemModifier.listItemHeight()),
                verticalArrangement = listItemCenterSpacedByHalf,
            ) {
                Text(
                    worksite.caseNumber,
                    style = LocalFontStyles.current.header3,
                )

                WorksiteNameView(worksite.name)

                WorksiteAddressView(fullAddress) {
                    if (worksite.hasWrongLocationFlag) {
                        ExplainWrongLocationDialog(worksite)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = listItemSpacedBy,
                ) {
                    WorksiteCallButton(
                        phone1 = worksite.phone1,
                        phone2 = worksite.phone2,
                        enable = true,
                        onShowPhoneNumbers = showPhoneNumbers,
                    )

                    WorksiteAddressButton(
                        geoQuery = geoQuery,
                        locationQuery = locationQuery,
                        isEditable = true,
                    )
                }
            }
        }
    }
}
