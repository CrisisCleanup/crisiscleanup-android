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
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.LinkifyEmailText
import com.crisiscleanup.core.designsystem.component.LinkifyPhoneText
import com.crisiscleanup.core.designsystem.component.PhoneNumbersDialog
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.component.WorksiteAddressButton
import com.crisiscleanup.core.designsystem.component.WorksiteAddressView
import com.crisiscleanup.core.designsystem.component.WorksiteCallButton
import com.crisiscleanup.core.designsystem.component.WorksiteNameView
import com.crisiscleanup.core.designsystem.component.actionHeight
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemCenterSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.model.data.CrisisCleanupList
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
    viewModel: ViewListViewModel = hiltViewModel(),
) {
    val screenTitle by viewModel.screenTitle.collectAsStateWithLifecycle()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

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
                        rememberKey = viewModel,
                    )
                }

                is ViewListViewState.Error -> {
                    Text((viewState as ViewListViewState.Error).message)
                }

                else -> {}
            }
        }

        BusyIndicatorFloatingTopCenter(viewState is ViewListViewState.Loading)
    }
}

@Composable
private fun ListDetailsView(
    list: CrisisCleanupList,
    objectData: List<Any?>,
    rememberKey: Any,
) {
    val t = LocalAppTranslator.current

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
        PhoneNumbersDialog(
            parsedNumbers = phoneNumberList,
            onCloseDialog = clearPhoneNumbers,
        )

        // TODO Load individual items
        LazyColumn {
            when (list.model) {
                ListModel.Incident -> {
                    incidentItems(objectData)
                }

                ListModel.List -> {
                    // TODO Open to List
                    listItems(objectData)
                }

                ListModel.Organization -> {
                    organizationItems(objectData)
                }

                ListModel.User -> {
                    userItems(objectData)
                }

                ListModel.Worksite -> {
                    // TODO Open to Case
                    worksiteItems(
                        objectData,
                        setPhoneNumberList,
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun MissingItem() {
    Text(
        LocalAppTranslator.current("~~Missing list data."),
        listItemModifier,
    )
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
                    .actionHeight()
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
                listItemModifier
                    .actionHeight(),
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
        if (worksite == null) {
            MissingItem()
        } else {
            val (fullAddress, geoQuery, locationQuery) = worksite.addressQuery

            Column(
                Modifier
                    .clickable(onClick = { onOpenWorksite(worksite) })
                    .then(
                        listItemModifier
                            .actionHeight(),
                    ),
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
