package com.crisiscleanup.feature.team.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.AvatarIcon
import com.crisiscleanup.core.designsystem.component.CardSurface
import com.crisiscleanup.core.designsystem.component.TopBarBackAction
import com.crisiscleanup.core.designsystem.theme.LocalDimensions
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemBottomPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.listRowItemStartPadding
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.feature.team.ViewTeamViewModel

@Composable
fun ViewTeamRoute(
    onBack: () -> Unit,
) {
    ViewTeamScreen(
        onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewTeamScreen(
    onBack: () -> Unit,
    viewModel: ViewTeamViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val isBusy = isLoading || isSaving

    val isEditable = !isBusy

    val title = viewModel.headerTitle

    val team by viewModel.editableTeam.collectAsStateWithLifecycle()
    Box {
        Column {
            CenterAlignedTopAppBar(
                title = { },
                navigationIcon = { TopBarBackAction(onBack) },
                actions = { /* TODO */ },
            )

            ViewTeamContent(
                team = team,
                // TODO Is saving instead?
                isBusy = isBusy,
                isEditable = isEditable,
            )
        }
    }
}

// TODO Common styles
private val headerTextStyle: TextStyle
    @Composable @ReadOnlyComposable
    get() = LocalFontStyles.current.header3

@Composable
private fun EditSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    linkActionModifier: Modifier = Modifier,
    enabled: Boolean = false,
    showEditAction: Boolean = false,
    color: Color = primaryBlueColor,
    action: () -> Unit = {},
) {
    Row(
        Modifier.listRowItemStartPadding(),
        horizontalArrangement = listItemSpacedByHalf,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            modifier,
            style = headerTextStyle,
        )

        Spacer(Modifier.weight(1f))

        if (showEditAction) {
            Box(
                modifier = Modifier
                    .clickable(
                        enabled = enabled,
                        onClick = action,
                    )
                    .then(linkActionModifier),
            ) {
                Text(
                    LocalAppTranslator.current("actions.edit"),
                    // TODO Common dimensions
                    Modifier.padding(16.dp),
                    style = LocalFontStyles.current.header4,
                    color = if (enabled) color else color.disabledAlpha(),
                )
            }
        }
    }
}

@Composable
private fun ViewTeamContent(
    team: CleanupTeam,
    isBusy: Boolean,
    isEditable: Boolean,
    onEditTeamMembers: () -> Unit = {},
) {
    val t = LocalAppTranslator.current

    LazyColumn(
        verticalArrangement = listItemSpacedBy,
    ) {
        item {
            TeamHeader(team)
        }

        item(
            // TODO Key and type
        ) {
            val teamMemberSectionTitle = t("~~Team Members ({team_size})")
                .replace("{team_size}", "${team.members.count()}")
            EditSectionHeader(
                teamMemberSectionTitle,
                enabled = isEditable,
                showEditAction = true,
                action = onEditTeamMembers,
            )
        }

        items(
            team.members,
            key = { it.id },
            contentType = { "member-item" },
        ) {
            CardSurface {
                Row(
                    // TODO Common dimensions
                    Modifier.padding(12.dp),
                    horizontalArrangement = listItemSpacedByHalf,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(LocalDimensions.current.avatarCircleRadius)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        AvatarIcon(
                            // TODO Use correct profile picture
                            it.profilePictureUri,
                            it.fullName,
                        )
                    }

                    Text(
                        it.fullName,
                        style = LocalFontStyles.current.header4,
                    )
                    // TODO Sub text if web has subtext
                    // TODO Actions for calling
                }
            }
        }

        // TODO Assigned Cases

        // TODO Assets

        // TODO Statistics

        item {
            Spacer(Modifier.listItemBottomPadding())
        }
    }
}

@Composable
private fun TeamHeader(team: CleanupTeam) {
    val t = LocalAppTranslator.current
    Column(
        // TODO Common colors
        Modifier.background(Color.White)
            .then(fillWidthPadded),
        verticalArrangement = listItemSpacedByHalf,
    ) {
        Row(
            horizontalArrangement = listItemSpacedByHalf,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeamColorView(team.colorInt)

            Text(
                team.name.ifBlank { t("~~(no name)") },
                style = LocalFontStyles.current.header2,
            )

            // TODO Show if syncing or pending sync
        }

        val openCaseCount = team.openCaseCount
        Row(
            horizontalArrangement = listItemSpacedByHalf,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val caseCountTranslateKey =
                if (openCaseCount == 1) "~~{case_count} Open Case" else "~~{case_count} Open Cases"
            Text(
                t(caseCountTranslateKey)
                    .replace("{case_count}", "$openCaseCount"),
            )

            TeamCaseCompleteView(team)
        }
    }
}
