package com.crisiscleanup.core.commoncase.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import com.crisiscleanup.core.common.combineTrimText
import com.crisiscleanup.core.commoncase.model.CaseSummaryResult
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.theme.listItemOptionPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.optionItemPadding

@Composable
private fun CaseView(
    isTeamCasesSearch: Boolean,
    caseSummary: CaseSummaryResult,
    modifier: Modifier,
    isAssignEnabled: Boolean = false,
    onAssignCaseToTeam: () -> Unit = {},
) {
    val t = LocalAppTranslator.current

    Row(
        modifier,
        horizontalArrangement = listItemSpacedByHalf,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        with(caseSummary) {
            icon?.let {
                Image(
                    // TODO Cache image bitmap, prepareToDraw() as well
                    bitmap = it.asImageBitmap(),
                    contentDescription = summary.workType?.workTypeLiteral,
                )
            }
            Column(Modifier.weight(1f)) {
                with(summary) {
                    Text(listOf(name, caseNumber).combineTrimText())
                    Text(listOf(address, city, state).combineTrimText())
                }
            }

            if (isTeamCasesSearch) {
                CrisisCleanupButton(
                    enabled = isAssignEnabled,
                    text = t("actions.assign"),
                    onClick = onAssignCaseToTeam,
                )
            }
        }
    }
}

fun LazyListScope.listCaseResults(
    isTeamCasesSearch: Boolean,
    worksites: List<CaseSummaryResult>,
    onCaseSelect: (CaseSummaryResult) -> Unit = {},
    itemKey: (CaseSummaryResult) -> Any = { it.listItemKey },
    isEditable: Boolean = false,
) {
    items(
        worksites,
        key = itemKey,
        contentType = { "item-worksite" },
    ) {
        CaseView(
            isTeamCasesSearch = isTeamCasesSearch,
            it,
            Modifier
                .testTag("workSearchResultItem_${it.listItemKey}")
                .clickable(
                    enabled = isEditable && !isTeamCasesSearch,
                    onClick = { onCaseSelect(it) },
                )
                .listItemOptionPadding(),
            isAssignEnabled = isEditable,
        ) {
            if (isTeamCasesSearch) {
                onCaseSelect(it)
            }
        }
    }
}

@Composable
fun ExistingCaseLocationsDropdownItems(
    worksites: List<CaseSummaryResult>,
    onCaseSelect: (CaseSummaryResult) -> Unit = {},
) {
    worksites.forEach { caseLocation ->
        DropdownMenuItem(
            text = {
                CaseView(
                    isTeamCasesSearch = false,
                    caseLocation,
                    Modifier
                        .fillMaxWidth()
                        .optionItemPadding(),
                )
            },
            onClick = { onCaseSelect(caseLocation) },
        )
    }
}
