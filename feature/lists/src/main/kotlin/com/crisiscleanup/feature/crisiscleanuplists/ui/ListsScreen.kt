package com.crisiscleanup.feature.crisiscleanuplists.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.crisiscleanup.core.common.relativeTime
import com.crisiscleanup.core.commonassets.ui.getDisasterIcon
import com.crisiscleanup.core.commoncase.ui.IncidentHeaderView
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.HelpDialog
import com.crisiscleanup.core.designsystem.component.TopAppBarBackCaretAction
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.icon.Icon
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemCenterSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.listItemHeight
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.primaryOrangeColor
import com.crisiscleanup.core.model.data.CrisisCleanupList
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.EmptyList
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.ListModel
import com.crisiscleanup.feature.crisiscleanuplists.ListsViewModel
import com.crisiscleanup.feature.crisiscleanuplists.model.ListIcon
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
)
@Composable
internal fun ListsRoute(
    onBack: () -> Unit = {},
    onOpenList: (CrisisCleanupList) -> Unit = {},
    viewModel: ListsViewModel = hiltViewModel(),
) {
    val t = LocalAppTranslator.current

    val incidentLists by viewModel.incidentLists.collectAsStateWithLifecycle()
    val allLists = viewModel.allLists.collectAsLazyPagingItems()

    val tabTitles = remember(incidentLists, allLists.itemCount) {
        val incidentText = t("list.incident")
        val allText = t("actions.all")
        val listCount = allLists.itemCount
        listOf(
            if (incidentLists.isEmpty()) incidentText else "$incidentText (${incidentLists.size})",
            if (listCount == 0) allText else "$allText ($listCount)",
        )
    }

    val isLoading by viewModel.isRefreshingData.collectAsStateWithLifecycle()

    val currentIncident by viewModel.currentIncident.collectAsStateWithLifecycle()

    var showReadOnlyDescription by remember { mutableStateOf(false) }

    Column {
        TopAppBarBackCaretAction(
            title = t("list.lists"),
            onAction = onBack,
            actions = {
                CrisisCleanupIconButton(
                    imageVector = CrisisCleanupIcons.Info,
                    onClick = {
                        showReadOnlyDescription = true
                    },
                    enabled = true,
                )
            },
        )

        val coroutineScope = rememberCoroutineScope()
        var isRefreshingLists by remember { mutableStateOf(false) }
        val refreshLists = remember(viewModel) {
            {
                coroutineScope.launch {
                    isRefreshingLists = true
                    try {
                        viewModel.refreshLists(true)
                    } finally {
                        isRefreshingLists = false
                    }
                }
                Unit
            }
        }

        val pagerState = rememberPagerState(
            initialPage = 0,
            initialPageOffsetFraction = 0f,
        ) { tabTitles.size }

        val openAllListsTab by viewModel.openAllListsTab.collectAsStateWithLifecycle(false)
        LaunchedEffect(openAllListsTab) {
            if (openAllListsTab) {
                pagerState.scrollToPage(1)
            }
        }

        val selectedTabIndex = pagerState.currentPage
        val coroutine = rememberCoroutineScope()
        TabRow(
            selectedTabIndex = selectedTabIndex,
            indicator = @Composable { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    // TODO Common dimensions
                    height = 2.dp,
                    color = primaryOrangeColor,
                )
            },
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    text = {
                        Text(
                            title,
                            style = LocalFontStyles.current.header4,
                        )
                    },
                    selected = selectedTabIndex == index,
                    onClick = {
                        coroutine.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    modifier = Modifier.testTag("listTab_$index"),
                )
            }
        }

        var explainSupportList by remember { mutableStateOf(EmptyList) }
        val filterOnOpenList = remember(onOpenList) {
            { list: CrisisCleanupList ->
                when (list.model) {
                    ListModel.None,
                    ->
                        explainSupportList = list

                    else ->
                        onOpenList(list)
                }
            }
        }

        PullToRefreshBox(
            modifier = Modifier
                .weight(1f),
            isRefreshing = isRefreshingLists,
            onRefresh = refreshLists,
        ) {
            HorizontalPager(state = pagerState) { pagerIndex ->
                when (pagerIndex) {
                    0 -> IncidentListsView(
                        incidentLists,
                        currentIncident,
                        filterOnOpenList,
                    )

                    1 -> AllListsView(
                        allLists,
                        filterOnOpenList,
                    )
                }
            }

            BusyIndicatorFloatingTopCenter(isLoading)
        }

        if (explainSupportList != EmptyList) {
            val dismissExplanation = { explainSupportList = EmptyList }
            // TODO Different title and message for list type none
            HelpDialog(
                title = t("list.unsupported_list_title"),
                text = t("list.unsupported_list_explanation")
                    .replace("{list_name}", explainSupportList.name),
                onClose = dismissExplanation,
            )
        }
    }

    if (showReadOnlyDescription) {
        val readOnlyTitle = t("list.list_read_only")
        val readOnlyDescription =
            t("list.read_only_in_app_manage_on_web")
        HelpDialog(
            title = readOnlyTitle,
            text = readOnlyDescription,
            onClose = { showReadOnlyDescription = false },
        )
    }
}

@Composable
private fun IncidentListsView(
    incidentLists: List<CrisisCleanupList>,
    incident: Incident,
    onOpenList: (CrisisCleanupList) -> Unit = {},
) {
    val t = LocalAppTranslator.current

    val listState = rememberLazyListState()
    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = listItemSpacedByHalf,
    ) {
        if (incident != EmptyIncident) {
            item(key = "incident-info") {
                IncidentHeaderView(
                    Modifier,
                    incident.shortName,
                    getDisasterIcon(incident.disaster),
                    isSyncing = false,
                )

                LaunchedEffect(Unit) {
                    listState.scrollToItem(0)
                }
            }
        }

        if (incidentLists.isEmpty()) {
            item(key = "static-text") {
                Text(
                    t("list.no_lists_for_this_incident"),
                    listItemModifier,
                )
            }
        } else {
            items(
                incidentLists.size,
                key = { incidentLists[it].id },
                contentType = { "list-item" },
            ) { index ->
                val list = incidentLists[index]
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
}

@Composable
private fun AllListsView(
    pagingLists: LazyPagingItems<CrisisCleanupList>,
    onOpenList: (CrisisCleanupList) -> Unit = {},
) {
    val t = LocalAppTranslator.current

    val listState = rememberLazyListState()
    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = listItemSpacedByHalf,
    ) {
        if (pagingLists.itemCount == 0) {
            item {
                Text(
                    t("list.create_new_in_browser"),
                    listItemModifier,
                )
            }
        }

        items(
            pagingLists.itemCount,
            key = pagingLists.itemKey { it.id },
            contentType = { "list-item" },
        ) { index ->
            val list = pagingLists[index]
            if (list == null) {
                Text(
                    "$index",
                    Modifier.listItemPadding(),
                )
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

        if (pagingLists.loadState.append is LoadState.Loading) {
            item(
                contentType = { "loading" },
            ) {
                // TODO Loading indicator
            }
        }
    }
}

@Composable
internal fun ListIcon(
    list: CrisisCleanupList,
) {
    val icon = list.ListIcon
    val contentDescription = list.model.literal
    when (icon) {
        is Icon.ImageVectorIcon -> Icon(
            imageVector = icon.imageVector,
            contentDescription = contentDescription,
        )

        is Icon.DrawableResourceIcon -> {
            Icon(
                painter = painterResource(icon.id),
                contentDescription = contentDescription,
            )
        }
    }
}

@Composable
internal fun ListItemSummaryView(
    list: CrisisCleanupList,
    modifier: Modifier = Modifier,
    showIncident: Boolean = false,
) {
    Column(
        modifier.listItemHeight(),
        verticalArrangement = listItemCenterSpacedByHalf,
    ) {
        Row(horizontalArrangement = listItemSpacedByHalf) {
            ListIcon(list)

            Text(
                "${list.name} (${list.objectIds.size})",
                Modifier.weight(1f),
                style = LocalFontStyles.current.header3,
            )
            Text(list.updatedAt.relativeTime)
        }

        val incidentName = if (showIncident) list.incident?.shortName ?: "" else ""
        val description = list.description.trim()
        if (incidentName.isNotBlank() || description.isNotBlank()) {
            Row {
                if (description.isNotBlank()) {
                    Text(
                        description,
                        Modifier.weight(1f),
                    )
                }

                if (showIncident) {
                    if (description.isBlank()) {
                        Spacer(Modifier.weight(1f))
                    }
                    if (incidentName.isNotBlank()) {
                        Text(incidentName)
                    }
                }
            }
        }
    }
}
