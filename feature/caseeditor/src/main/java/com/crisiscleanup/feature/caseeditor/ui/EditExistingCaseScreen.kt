package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.AnimatedBusyIndicator
import com.crisiscleanup.core.designsystem.theme.neutralIconColor
import com.crisiscleanup.core.designsystem.theme.primaryRedIconColor
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
            toggleFavorite,
            toggleHighPriority,
        )

        // TODO Show message if empty worksite ID was passed as arg

        val isEmptyWorksite = worksite == EmptyWorksite
        if (isEmptyWorksite) {

        } else {
            // TODO TabView
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
    // TODO Translations if exist
    val actionsContent: (@Composable (RowScope.() -> Unit)) = @Composable {
        IconButton(
            onClick = toggleFavorite,
        ) {
            val iconResId = if (isFavorite) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
            val descriptionResId = if (isFavorite) R.string.not_favorite
            else R.string.favorite
            val tint = if (isFavorite) primaryRedIconColor
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
            val tint = if (isHighPriority) primaryRedIconColor
            else neutralIconColor
            Icon(
                painter = painterResource(R.drawable.ic_important_filled),
                contentDescription = stringResource(descriptionResId),
                tint = tint,
            )
        }
    }
    CenterAlignedTopAppBar(
        title = titleContent,
        navigationIcon = navigationContent,
        actions = actionsContent,
//        colors = TopAppBarDefaults.centerAlignedTopAppBarColors,
    )
}

@Composable
internal fun EditCaseInfoView(
    worksite: Worksite,
) {
    if (worksite == EmptyWorksite) {
        Box(Modifier.fillMaxSize()) {
            AnimatedBusyIndicator(true)
        }
    } else {
        Text("Worksite ${worksite.caseNumber}")
    }
}