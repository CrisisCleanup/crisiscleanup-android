package com.crisiscleanup.feature.team.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.appcomponent.ui.AppTopBar
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.AvatarIcon
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CardSurface
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.theme.LocalDimensions
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemBottomPadding
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.listItemTopPadding
import com.crisiscleanup.core.designsystem.theme.neutralBackgroundColor
import com.crisiscleanup.core.designsystem.theme.neutralFontColor
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.selectincident.SelectIncidentDialog
import com.crisiscleanup.feature.team.TeamsViewModel
import com.crisiscleanup.feature.team.TeamsViewState

@Composable
internal fun TeamsRoute(
    openAuthentication: () -> Unit = {},
    openViewTeam: (Long) -> Unit = {},
    openCreateTeam: () -> Unit = {},
    openTeamFilters: () -> Unit = {},
) {
    TeamsScreen(
        openAuthentication = openAuthentication,
        openViewTeam = openViewTeam,
        openCreateTeam = openCreateTeam,
        openTeamFilters = openTeamFilters,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamsScreen(
    viewModel: TeamsViewModel = hiltViewModel(),
    openAuthentication: () -> Unit = {},
    openViewTeam: (Long) -> Unit = {},
    openCreateTeam: () -> Unit = {},
    openTeamFilters: () -> Unit = {},
) {
    val t = LocalAppTranslator.current

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle(true)

    val incidentsData by viewModel.incidentsData.collectAsStateWithLifecycle()

    var showIncidentPicker by remember { mutableStateOf(false) }
    val openIncidentsSelect = remember(viewModel) {
        { showIncidentPicker = true }
    }

    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    val profilePictureLookup by viewModel.profilePictureLookup.collectAsStateWithLifecycle()

    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refreshTeams()
            pullRefreshState.endRefresh()
        }
    }

    Box {
        Column {
            // TODO Modifiers and test tag
            AppTopBar(
                dataProvider = viewModel.appTopBarDataProvider,
                openAuthentication = openAuthentication,
                onOpenIncidents = openIncidentsSelect,
            )

            if (viewState is TeamsViewState.Success) {
                val successState = viewState as TeamsViewState.Success
                val incidentTeams = successState.teams
                val listState = rememberLazyListState()
                LazyColumn(
                    modifier = Modifier
                        .nestedScroll(pullRefreshState.nestedScrollConnection)
                        .fillMaxHeight(),
                    state = listState,
                    verticalArrangement = listItemSpacedBy,
                ) {
                    item(
                        key = "my-teams",
                        contentType = "title-text-item",
                    ) {
                        Text(
                            text = t("teams.my_teams"),
                            modifier = listItemModifier,
                            style = LocalFontStyles.current.header1,
                        )
                    }

                    if (incidentTeams.myTeams.isEmpty()) {
                        item(
                            key = "not-in-teams",
                            contentType = "subtitle-text-item",
                        ) {
                            Text(
                                text = t("teams.no_team_yet"),
                                modifier = Modifier.listItemHorizontalPadding(),
                                style = LocalFontStyles.current.header2,
                            )
                        }
                    } else {
                        items(
                            incidentTeams.myTeams,
                            key = { it.id },
                            contentType = { "team-item" },
                        ) {
                            TeamView(
                                it,
                                profilePictureLookup,
                                openViewTeam,
                            )
                        }
                    }

                    item(
                        key = "create-team",
                        contentType = "primary-action",
                    ) {
                        CrisisCleanupButton(
                            modifier = Modifier.listItemPadding(),
                            text = t("teams.create_new_team"),
                            onClick = openCreateTeam,
                            enabled = false,
                        )
                    }

                    if (incidentTeams.otherTeams.isEmpty()) {
                        item(
                            key = "no-other-team",
                            contentType = "subtitle-text-item",
                        ) {
                            Text(
                                text = t("teams.no_other_teams"),
                                modifier = Modifier.listItemHorizontalPadding()
                                    .listItemTopPadding(),
                                style = LocalFontStyles.current.header2,
                            )
                        }
                    } else {
                        item(
                            key = "join-team",
                            contentType = "subtitle-text-item",
                        ) {
                            Text(
                                text = t("teams.join_team"),
                                modifier = Modifier.listItemHorizontalPadding()
                                    .listItemTopPadding(),
                                style = LocalFontStyles.current.header2,
                            )
                        }

                        // TODO Search and filter

                        items(
                            incidentTeams.otherTeams,
                            key = { it.id },
                            contentType = { "team-item" },
                        ) {
                            TeamView(
                                it,
                                profilePictureLookup,
                                openViewTeam,
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.listItemBottomPadding())
                    }
                }
            }
        }

        BusyIndicatorFloatingTopCenter(isLoading)

        PullToRefreshContainer(
            modifier = Modifier
                .align(Alignment.TopCenter),
            state = pullRefreshState,
        )
    }

    if (showIncidentPicker) {
        val closeDialog = { showIncidentPicker = false }
        val selectedIncidentId by viewModel.incidentSelector.incidentId.collectAsStateWithLifecycle()
        val setSelected = remember(viewModel) {
            { incident: Incident ->
                viewModel.incidentSelector.selectIncident(incident)
            }
        }
        SelectIncidentDialog(
            rememberKey = viewModel,
            onBackClick = closeDialog,
            incidentsData = incidentsData,
            selectedIncidentId = selectedIncidentId,
            onSelectIncident = setSelected,
            onRefreshIncidentsAsync = viewModel::refreshIncidentsAsync,
        )
    }
}

@Composable
internal fun TeamView(
    team: CleanupTeam,
    profilePictureLookup: Map<Long, String>,
    openViewTeam: (Long) -> Unit = {},
) {
    val t = LocalAppTranslator.current

    CardSurface {
        Column(
            Modifier
                .clickable(onClick = { openViewTeam(team.id) })
                .then(listItemModifier),
            verticalArrangement = listItemSpacedByHalf,
        ) {
            Row(horizontalArrangement = listItemSpacedBy) {
                Text(
                    team.name,
                    Modifier.weight(1f),
                    style = LocalFontStyles.current.header4,
                )
            }

            val caseCount = team.caseCount
            Row(horizontalArrangement = listItemSpacedBy) {
                val caseCountTranslateKey =
                    if (caseCount == 1) "teams.one_case" else "teams.case_count_cases"
                Text(
                    t(caseCountTranslateKey)
                        .replace("{case_count}", "$caseCount"),
                )

                if (team.caseCompletePercentage > 0) {
                    Text(
                        t("teams.percent_complete_cases_completed")
                            .replace("{percent_complete}", "${team.caseCompletePercentage}"),
                        color = neutralFontColor,
                    )
                }
            }

            val memberCount = team.members.size
            // TODO Or has equipment
            if (memberCount > 0) {
                Row(horizontalArrangement = listItemSpacedByHalf) {
                    team.members.forEachIndexed { i, contact ->
                        if (i > 2) {
                            return@forEachIndexed
                        }

                        Box(
                            modifier = Modifier
                                .size(LocalDimensions.current.avatarCircleRadius)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            AvatarIcon(
                                profilePictureLookup[contact.id],
                                contact.fullName,
                            )
                        }
                    }

                    if (memberCount > 2) {
                        Box(
                            modifier = Modifier
                                .size(LocalDimensions.current.avatarCircleRadius)
                                .clip(CircleShape)
                                .background(neutralBackgroundColor),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("+${memberCount - 3}")
                        }
                    }

                    // TODO equipment, and count
                }
            }
        }
    }
}
