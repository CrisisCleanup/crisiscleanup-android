package com.crisiscleanup.feature.team.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.HeaderSubTitle
import com.crisiscleanup.core.designsystem.component.HeaderTitle
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.TopBarBackAction
import com.crisiscleanup.core.designsystem.component.actionHeight
import com.crisiscleanup.core.designsystem.theme.LocalDimensions
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.listItemTopPadding
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor
import com.crisiscleanup.core.designsystem.theme.primaryOrangeColor
import com.crisiscleanup.core.model.data.CleanupTeam
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

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isEditable = !isLoading

    val editingTeam by viewModel.editingTeam.collectAsStateWithLifecycle()

    Column {
        TeamEditorHeader(
            title = viewModel.headerTitle,
            subTitle = viewModel.headerSubTitle,
            onCancel = onBack,
        )

        Box(Modifier.fillMaxSize()) {
            if (tabState.titles.isNotEmpty()) {
                CreateEditTeamContent(
                    tabState.titles,
                    editingTeam,
                    tabState.startingIndex,
                    isEditable = isEditable,
                    teamName = viewModel.editingTeamName,
                    onTeamNameChange = viewModel::onTeamNameChange,
                    onSuggestName = viewModel::onSuggestTeamName,
                )
            }

            BusyIndicatorFloatingTopCenter(isLoading)
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
private fun CreateEditTeamContent(
    tabTitles: List<String>,
    team: CleanupTeam,
    initialPage: Int,
    isEditable: Boolean,
    teamName: String,
    onTeamNameChange: (String) -> Unit,
    onSuggestName: () -> Unit,
) {
    // TODO Page does not keep across first orientation change
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        initialPageOffsetFraction = 0f,
    ) { tabTitles.size }
    val selectedTabIndex = pagerState.currentPage
    val coroutine = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
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

        HorizontalPager(
            pagerState,
            Modifier.fillMaxSize(),
            userScrollEnabled = enablePagerScroll,
        ) { pagerIndex ->
            when (pagerIndex) {
                0 -> EditTeamNameView(
                    team.colorInt,
                    teamName,
                    isEditable,
                    onTeamNameChange,
                    onSuggestName,
                )

                1 -> EditTeamMembersView(team)
                2 -> EditTeamCasesView(team)
                3 -> EditTeamEquipmentView(team)
                4 -> ReviewChangesView(team)
            }
        }
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
    teamColorInt: Int,
    name: String,
    isEditable: Boolean,
    onTeamNameChange: (String) -> Unit,
    onSuggestName: () -> Unit,
) {
    val t = LocalAppTranslator.current

    val closeKeyboard = rememberCloseKeyboard(onTeamNameChange)

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = listItemSpacedByHalf,
    ) {
        Box(listItemModifier.listItemTopPadding()) {
            TeamColorView(
                teamColorInt,
            )
        }

        OutlinedClearableTextField(
            modifier = listItemModifier
                .testTag("teamEditorNameTextField"),
            label = t("~~Team name"),
            value = name,
            onValueChange = { onTeamNameChange(it) },
            keyboardType = KeyboardType.Password,
            enabled = isEditable,
            isError = false,
            keyboardCapitalization = KeyboardCapitalization.Words,
            onEnter = closeKeyboard,
            imeAction = ImeAction.Done,
        )

        val color = if (isEditable) {
            primaryBlueColor
        } else {
            primaryBlueColor.disabledAlpha()
        }
        Box(
            Modifier
                .clickable(
                    onClick = onSuggestName,
                )
                .listItemPadding()
                .actionHeight()
                .align(Alignment.End)
                .testTag("teamEditorSuggestNameAction"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                t("~~Suggest a name"),
                color = color,
                style = LocalFontStyles.current.header3,
            )
        }

        Modifier.weight(1f)
    }
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
