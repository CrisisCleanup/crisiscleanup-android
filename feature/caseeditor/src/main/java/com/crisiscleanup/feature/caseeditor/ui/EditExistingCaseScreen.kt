package com.crisiscleanup.feature.caseeditor.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupNavigationDefaults
import com.crisiscleanup.core.designsystem.theme.*
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.ExistingCaseViewModel
import com.crisiscleanup.feature.caseeditor.ExistingWorksiteIdentifier
import com.crisiscleanup.feature.caseeditor.R

@Composable
internal fun EditExistingCaseRoute(
    viewModel: ExistingCaseViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onFullEdit: (ExistingWorksiteIdentifier) -> Unit = {},
) {
    val worksite by viewModel.worksite.collectAsStateWithLifecycle()
    val isEmptyWorksite = worksite == EmptyWorksite

    val toggleFavorite = remember(viewModel) { { viewModel.toggleFavorite() } }
    val toggleHighPriority = remember(viewModel) { { viewModel.toggleHighPriority() } }
    Column {
        val title by viewModel.headerTitle.collectAsStateWithLifecycle()
        val subTitle by viewModel.subTitle.collectAsStateWithLifecycle()
        TopBar(
            title,
            subTitle,
            isFavorite = worksite.isFavorited,
            isHighPriority = worksite.hasHighPriorityFlag,
            onBack,
            isEmptyWorksite,
            toggleFavorite,
            toggleHighPriority,
        )

        if (isEmptyWorksite) {
            if (viewModel.worksiteIdArg == EmptyWorksite.id) {
                Text(
                    stringResource(R.string.no_worksite_selected),
                    Modifier.listItemPadding(),
                )
            } else {
                Box(Modifier.fillMaxSize()) {
                    BusyIndicatorFloatingTopCenter(true)
                }
            }
        } else {
            val translate = remember(viewModel) { { s: String -> viewModel.translate(s) } }

            ExistingCaseContent(
                worksite,
                translate,
            )

            BottomActions(
                worksite,
                translate,
                onFullEdit,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    title: String,
    subTitle: String = "",
    isFavorite: Boolean = false,
    isHighPriority: Boolean = false,
    onBack: () -> Unit = {},
    isLoading: Boolean = false,
    toggleFavorite: () -> Unit = {},
    toggleHighPriority: () -> Unit = {},
) {
    // TODO Style components as necessary

    val titleContent = @Composable {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title)

            if (subTitle.isNotBlank()) {
                Text(
                    subTitle,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    val navigationContent = @Composable {
        Text(
            stringResource(R.string.back),
            Modifier
                .clickable(onClick = onBack)
                .padding(8.dp),
        )
    }
    val actionsContent: (@Composable (RowScope.() -> Unit)) = if (isLoading) {
        @Composable {}
    } else {
        @Composable {
            // TODO Translations if exist

            IconButton(
                onClick = toggleFavorite,
            ) {
                val iconResId = if (isFavorite) R.drawable.ic_heart_filled
                else R.drawable.ic_heart_outline
                val descriptionResId = if (isFavorite) R.string.not_favorite
                else R.string.favorite
                val tint = if (isFavorite) primaryRedColor
                else neutralIconColor
                Icon(
                    painter = painterResource(iconResId),
                    contentDescription = stringResource(descriptionResId),
                    tint = tint,
                )
            }
            IconButton(
                onClick = toggleHighPriority,
            ) {
                val descriptionResId = if (isHighPriority) R.string.not_high_priority
                else R.string.high_priority
                val tint = if (isHighPriority) primaryRedColor
                else neutralIconColor
                Icon(
                    painter = painterResource(R.drawable.ic_important_filled),
                    contentDescription = stringResource(descriptionResId),
                    tint = tint,
                )
            }
        }
    }
    CenterAlignedTopAppBar(
        title = titleContent,
        navigationIcon = navigationContent,
        actions = actionsContent,
//        colors = TopAppBarDefaults.centerAlignedTopAppBarColors,
    )
}

// TODO Translate if available
private val tabTitles = listOf(
    R.string.info,
    R.string.photos,
    R.string.notes,
    R.string.release,
)

@Composable
private fun ColumnScope.ExistingCaseContent(
    worksite: Worksite,
    translate: (String) -> String? = { null },
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    TabRow(
        selectedTabIndex = selectedTabIndex,
        indicator = @Composable { tabPositions ->
            TabRowDefaults.Indicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                // TODO Common dimensions
                height = 2.dp,
                color = primaryOrangeColor,
            )
        },
    ) {
        tabTitles.forEachIndexed { index, titleResId ->
            Tab(
                text = { Text(stringResource(titleResId)) },
                selected = selectedTabIndex == index,
                onClick = { selectedTabIndex = index },
            )
        }
    }

    Box(Modifier.weight(1f)) {
        // TODO Tab content
        when (selectedTabIndex) {
            0 -> {}
            1 -> {}
            2 -> {}
            3 -> {}
        }
    }
}

@Composable
private fun BottomActions(
    worksite: Worksite,
    translate: (String) -> String? = { null },
    onFullEdit: (ExistingWorksiteIdentifier) -> Unit = {},
) {
    val contentColor = Color.Black
    NavigationBar(
        containerColor = Color.White,
        contentColor = contentColor,
        tonalElevation = 0.dp,
    ) {
        existingCaseActions.forEachIndexed { index, action ->
            var label = action.text
            if (action.translationKey.isNotBlank()) {
                translate(action.translationKey)?.let {
                    label = it
                }
            }
            if (label.isBlank() && action.textResId != 0) {
                label = stringResource(action.textResId)
            }

            NavigationBarItem(
                selected = false,
                onClick = {
                    when (index) {
                        0 -> {}
                        1 -> {}
                        2 -> {}
                        3 -> {
                            onFullEdit(ExistingWorksiteIdentifier(worksite.incidentId, worksite.id))
                        }
                    }
                },
                icon = {
                    if (action.iconResId != 0) {
                        Icon(
                            painter = painterResource(action.iconResId),
                            contentDescription = label,
                        )
                    } else if (action.imageVector != null) {
                        Icon(
                            imageVector = action.imageVector,
                            contentDescription = label,
                        )
                    }
                },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = contentColor,
                    unselectedTextColor = contentColor,
                    indicatorColor = CrisisCleanupNavigationDefaults.navigationIndicatorColor(),
                )
            )
        }
    }
}

@Composable
internal fun EditExistingCaseInfoView(
    worksite: Worksite,
) {
    if (worksite == EmptyWorksite) {
        Box(Modifier.fillMaxSize()) {
            BusyIndicatorFloatingTopCenter(true)
        }
    } else {
        LazyColumn {
            item {
                Text("Worksite ${worksite.caseNumber}")
            }
            item {
                SectionHeader(
                    sectionIndex = 0,
                    sectionTitle = "Title",
                )
            }
        }
    }
}

data class IconTextAction(
    @DrawableRes val iconResId: Int = 0,
    val imageVector: ImageVector? = null,
    @StringRes val textResId: Int = 0,
    val text: String = "",
    val translationKey: String = "",
)

private val existingCaseActions = listOf(
    IconTextAction(
        iconResId = R.drawable.ic_share_small,
        textResId = R.string.share,
    ),
    IconTextAction(
        iconResId = R.drawable.ic_flag_small,
        textResId = R.string.flag,
    ),
    IconTextAction(
        iconResId = R.drawable.ic_history_small,
        textResId = R.string.history,
    ),
    IconTextAction(
        iconResId = R.drawable.ic_edit_underscore_small,
        textResId = R.string.edit,
    ),
)