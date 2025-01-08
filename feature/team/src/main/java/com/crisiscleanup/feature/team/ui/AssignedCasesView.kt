package com.crisiscleanup.feature.team.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.crisiscleanup.core.commoncase.ui.CaseView
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupOutlinedButton
import com.crisiscleanup.core.designsystem.component.OutlinedSingleLineTextField
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listItemOptionPadding
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.model.data.Worksite

@Composable
fun AssignedCasesView(
    isLoadingCases: Boolean,
    assignedCases: List<Worksite>,
    iconProvider: MapCaseIconProvider,
    modifier: Modifier = Modifier,
    onSearchCases: () -> Unit = {},
    onViewCase: (Worksite) -> Unit = {},
    onUnassignCase: (Worksite) -> Unit = {},
) {
    val t = LocalAppTranslator.current

    Column(modifier) {
        OutlinedSingleLineTextField(
            modifier = Modifier
                .fillMaxWidth()
                .listItemPadding()
                .clickable(
                    onClick = onSearchCases,
                ),
            labelResId = 0,
            label = t("actions.search"),
            value = "",
            onValueChange = {},
            enabled = false,
            isError = false,
            leadingIcon = {
                Icon(
                    CrisisCleanupIcons.Search,
                    contentDescription = t("actions.search"),
                )
            },
            readOnly = true,
        )

        if (!isLoadingCases && assignedCases.isEmpty()) {
            Text(
                t("~~No Cases have been assigned to this team."),
                Modifier.listItemPadding(),
            )
        }

        LazyColumn(Modifier.weight(1f)) {
            items(
                assignedCases,
                key = { it.id },
                contentType = { "item-case" },
            ) {
                with(it) {
                    val icon = keyWorkType?.let { keyWorkType ->
                        iconProvider.getIconBitmap(
                            keyWorkType.statusClaim,
                            keyWorkType.workType,
                            hasMultipleWorkTypes = workTypes.size > 1,
                        )
                    }
                    CaseView(
                        icon,
                        keyWorkType,
                        name = name,
                        caseNumber = caseNumber,
                        address = address,
                        city = city,
                        state = state,
                        Modifier.clickable(
                            onClick = { onViewCase(it) },
                        )
                            .listItemOptionPadding(),
                    ) {
                        CrisisCleanupOutlinedButton(
                            text = t("actions.unassign"),
                            enabled = true,
                            onClick = { onUnassignCase(this) },
                        )
                    }
                }
            }
        }
    }
}
