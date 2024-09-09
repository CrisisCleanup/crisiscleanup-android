package com.crisiscleanup.feature.team.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.HeaderSubTitle
import com.crisiscleanup.core.designsystem.component.HeaderTitle
import com.crisiscleanup.core.designsystem.component.TopBarBackAction
import com.crisiscleanup.core.designsystem.theme.LocalDimensions
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.primaryOrangeColor
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.EmptyCleanupTeam
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.feature.team.CreateEditTeamViewModel
import kotlinx.coroutines.launch

@Composable
fun CreateEditTeamRoute(
    onBack: () -> Unit,
) {
    CreateEditTeamView(onBack)
}

@Composable
private fun CreateEditTeamView(
    onBack: () -> Unit,
    viewModel: CreateEditTeamViewModel = hiltViewModel(),
) {
    val tabState by viewModel.stepTabState.collectAsStateWithLifecycle()

    Column {
        TeamEditorHeader(
            title = viewModel.screenTitle,
            subTitle = viewModel.screenSubTitle,
            onCancel = onBack,
        )

        if (tabState.titles.isNotEmpty()) {
            CreateEditTeamContent(
                tabState.titles,
                EmptyCleanupTeam,
                false,
                tabState.startingIndex,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TeamEditorHeader(
    title: String,
    subTitle: String = "",
    onCancel: () -> Unit = {},
) {
    val titleContent = @Composable {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeaderTitle(
                title,
                Modifier.testTag("teamEditorHeaderTitle"),
            )
            HeaderSubTitle(
                subTitle,
                Modifier.testTag("teamEditorHeaderSubTitle"),
            )
        }
    }

    val navigationContent = @Composable { TopBarBackAction(onCancel) }

    CenterAlignedTopAppBar(
        title = titleContent,
        navigationIcon = navigationContent,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColumnScope.CreateEditTeamContent(
    tabTitles: List<String>,
    team: CleanupTeam,
    isLoading: Boolean,
    initialPage: Int,
) {
    // TODO Page does not keep across first orientation change
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        initialPageOffsetFraction = 0f,
    ) { tabTitles.size }
    val selectedTabIndex = pagerState.currentPage
    val coroutine = rememberCoroutineScope()
    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        indicator = @Composable { tabPositions ->
            SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                height = LocalDimensions.current.tabIndicatorHeight,
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
                modifier = Modifier.testTag("caseInfoTab_$title"),
            )
        }
    }

    var enablePagerScroll by remember { mutableStateOf(true) }
    val setEnablePagerScroll = remember(pagerState) { { b: Boolean -> enablePagerScroll = b } }

    Box(Modifier.weight(1f)) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = enablePagerScroll,
        ) { pagerIndex ->
            when (pagerIndex) {
                0 -> EditTeamNameView(team)
                1 -> EditTeamMembersView(team)
                2 -> EditTeamCasesView(team)
                3 -> EditTeamEquipmentView(team)
                4 -> ReviewChangesView(team)
            }
        }
        BusyIndicatorFloatingTopCenter(isLoading)
    }

    val closeKeyboard = rememberCloseKeyboard(pagerState)
    val pagerPage by remember(pagerState) {
        derivedStateOf {
            pagerState.currentPage
        }
    }
    LaunchedEffect(pagerPage) {
        closeKeyboard()
    }
}

@Composable
private fun EditTeamNameView(
    team: CleanupTeam,
) {
    // TODO Auto assign name as well
    Text("Edit name ${team.name}")
}

@Composable
private fun EditTeamMembersView(
    team: CleanupTeam,
) {
    Text("Edit members ${team.members.size}")
}

@Composable
private fun EditTeamCasesView(
    team: CleanupTeam,
) {
    Text("Edit cases ${team.caseCount}")
}

@Composable
private fun EditTeamEquipmentView(
    team: CleanupTeam,
) {
    Text("Edit equipment ${team.equipment.size}")
}

@Composable
private fun ReviewChangesView(
    team: CleanupTeam,
) {
    Text("Review team changes")
    // TODO Reuse views from team summary
}
